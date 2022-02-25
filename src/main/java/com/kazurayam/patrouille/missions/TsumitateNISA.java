package com.kazurayam.patrouille.missions;

import com.kazurayam.ashotwrapper.AShotWrapper;
import com.kazurayam.materialstore.MaterialstoreFacade;
import com.kazurayam.materialstore.MaterialstoreFacade.DiffResult;
import com.kazurayam.materialstore.filesystem.FileType;
import com.kazurayam.materialstore.filesystem.JobName;
import com.kazurayam.materialstore.filesystem.JobTimestamp;
import com.kazurayam.materialstore.filesystem.MaterialList;
import com.kazurayam.materialstore.filesystem.Store;
import com.kazurayam.materialstore.resolvent.ArtifactGroup;
import com.kazurayam.materialstore.metadata.Metadata;
import com.kazurayam.materialstore.metadata.QueryOnMetadata;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class TsumitateNISA extends AbstractMission {

    Logger logger = LoggerFactory.getLogger(TsumitateNISA.class);

    @Override
    public JobName jobName() {
        return new JobName("つみたてNISAの対象商品");
    }

    @Override
    public URL url() {
        try {
            return new URL("https://www.fsa.go.jp/policy/nisa2/about/tsumitate/target/index.html");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public JobTimestamp carryout(WebDriver browser, Store store) throws IOException {
        JobTimestamp  jobTimestamp = materialize(browser, this.url(), store);
        DiffResult diffResult = cook(store, this.jobName(), jobTimestamp, this.url());
        assert diffResult != null;
        return jobTimestamp;
    }

    private JobTimestamp materialize(WebDriver browser, URL baseURL, Store store) throws IOException {
        browser.navigate().to(baseURL);
        JobTimestamp jobTimestamp = JobTimestamp.now();

        // take the full-page screenshot image PNG
        File screenshotFile = Files.createTempFile(null, null).toFile();
        AShotWrapper.saveEntirePageImage(browser, screenshotFile);
        Metadata screenshotMetadata = Metadata.builderWithUrl(baseURL).build();
        store.write(this.jobName(), jobTimestamp,
                FileType.PNG, screenshotMetadata, screenshotFile);

        // download 4 EXCEL files
        List<By> targets = Arrays.asList(
                By.xpath("(//a[text()='EXCEL'])[1]"),
                By.xpath("(//a[text()='EXCEL'])[2]"),
                By.xpath("(//a[text()='EXCEL'])[3]"),
                By.xpath("(//a[text()='EXCEL'])[4]")
        );
        for (int i = 0; i < targets.size(); i++) {
            WebElement anchor = browser.findElement(targets.get(i));
            assert anchor != null;
            String relativeUrl = anchor.getAttribute("href");
            assert relativeUrl != null;
            logger.debug(String.format("[materialize] relative: %s", relativeUrl));
            URL resolved = resolveURL(baseURL, relativeUrl);
            logger.debug(String.format("[materialize] resolved: %s", resolved.toExternalForm()));
            Path tempFile = Files.createTempFile(null, null);
            assert tempFile != null;
            long size = download(resolved, tempFile);
            assert Files.exists(tempFile);
            assert tempFile.toFile().length() > 0;
            assert size == tempFile.toFile().length();
            Metadata excelMetadata =
                    Metadata.builderWithUrl(resolved)
                            .put("seq", (i + 1) + "")
                            .build();
            store.write(this.jobName(), jobTimestamp, FileType.XLSX,
                    excelMetadata, tempFile);
        }
        return jobTimestamp;
    }

    /**
     *
     */
    DiffResult cook(Store store, JobName jobName, JobTimestamp latestTimestamp, URL url) {
        Metadata metadata = Metadata.builderWithUrl(url).build();
        QueryOnMetadata query = QueryOnMetadata.builderWithMetadata(metadata).build();
        logger.debug(String.format("[resolve] jobName=%s", jobName.toString()));
        logger.debug(String.format("[resolve] url=%s", url.toString()));
        logger.debug(String.format("[resolve] metadata=%s", metadata.toString()));
        logger.debug(String.format("[resolve] query=%s", query.toString()));

        List<JobTimestamp> timestampsAll = store.queryAllJobTimestamps(jobName, query);
        assert timestampsAll.size() > 0;

        List<JobTimestamp> timestampsPriorTo = store.queryAllJobTimestampsPriorTo(jobName, query, latestTimestamp);
        assert timestampsPriorTo.size() > 0;

        JobTimestamp previousTimestamp =
                store.queryJobTimestampPriorTo(jobName, query, latestTimestamp);

        logger.debug(String.format("[resolve] latestTimestamp=%s", latestTimestamp.toString()));
        logger.debug(String.format("[resolve] previousTimestamp=%s", previousTimestamp.toString()));
        assert previousTimestamp != JobTimestamp.NULL_OBJECT;

        // look up the materials stored by the previous time of run
        MaterialList left = store.select(jobName, previousTimestamp, QueryOnMetadata.ANY);
        assert left.size() > 0;

        // look up the materials stored by the latest time of run
        MaterialList right = store.select(jobName, latestTimestamp, QueryOnMetadata.ANY);
        assert right.size() > 0;

        // the facade that work for you
        MaterialstoreFacade facade = MaterialstoreFacade.newInstance(store);

        ArtifactGroup prepared =
                ArtifactGroup.builder(left, right)
                        .ignoreKeys("URL.protocol", "URL.host", "URL.port")
                        .build();

        // if the difference is greater than this criteria value (unit %)
        // the difference should be marked
        double criteria = 0.1d;

        // the file name of HTML report
        String fileName = jobName.toString() + "-index.html";

        // make diff and compile report
        DiffResult diffResult = facade.makeDiffAndReport(jobName, prepared, criteria, fileName);

        assert Files.exists(diffResult.report());
        System.out.println("The report can be found at ${result.report()}");

        // if any significant difference found, should warn it
        int warnings = diffResult.warnings();
        if (warnings > 0) {
            System.err.println("found ${warnings} differences");
        }
        return diffResult;
    }

}
