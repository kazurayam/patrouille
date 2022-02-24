package com.kazurayam.patrouille;

import com.kazurayam.materialstore.MaterialstoreFacade;
import com.kazurayam.materialstore.MaterialstoreFacade.Result;
import com.kazurayam.materialstore.filesystem.*;
import com.kazurayam.materialstore.metadata.Metadata;
import com.kazurayam.materialstore.metadata.QueryOnMetadata;
import com.kazurayam.materialstore.resolvent.ArtifactGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Dessin {

    private static final Path projectDir = Paths.get(System.getProperty("user.dir"));
    private static Path outputDir;

    private WebDriver browser;
    private Store store;
    private JobName jobName;

    @BeforeAll
    public static void beforeAll() throws IOException {
        outputDir = projectDir.resolve("build/tmp/testOutput").resolve(Dessin.class.getName());
        Files.createDirectories(outputDir);
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setup() throws IOException {
        Path root = outputDir.resolve("store");
        /*
        if (Files.exists(root)) {
            Files.walk(root)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
         */
        Files.createDirectories(root);
        store = Stores.newInstance(outputDir.resolve("store"));
        browser = null;
    }

    @AfterEach
    public void tearDown() {
        if (browser != null) {
            browser.quit();
            browser = null;
        }
    }

    @Test
    public void test_smoke() throws Exception {
        jobName = new JobName("つみたてNISAの対象商品");
        JobTimestamp latestTimestamp = JobTimestamp.now();
        URL base = new URL("https://www.fsa.go.jp/policy/nisa2/about/tsumitate/target/index.html");
        browser = new ChromeDriver();
        browser.navigate().to(base);

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
            System.out.println(String.format("relativeUrl: %s", relativeUrl));
            URL resolved = resolveURL(base, relativeUrl);
            System.out.println(String.format("resolved   : %s", resolved.toExternalForm()));
            Path tempFile = Files.createTempFile(null, null);
            assert tempFile != null;
            download(resolved, tempFile);
            assert Files.exists(tempFile);
            assert tempFile.toFile().length() > 0;
            Metadata metadata =
                    Metadata.builderWithUrl(resolved)
                            .put("seq", (i + 1) + "")
                            .build();
            store.write(jobName, latestTimestamp, FileType.XLSX,
                    metadata, tempFile);

            // identify the 2nd latest jobTimestamp
            QueryOnMetadata query =
                    QueryOnMetadata.builderWithMetadata(metadata).build();
            JobTimestamp previousTimestamp =
                    store.queryJobTimestampPriorTo(jobName, query, latestTimestamp);
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
            Result result = facade.makeDiffAndReport(jobName, prepared, criteria, fileName);

            assert Files.exists(result.report());
            System.out.println("The report can be found at ${result.report()}");

            // if any significant difference found, should warn it
            int warnings = result.warnings();
            if (warnings > 0) {
                System.err.println("found ${warnings} differences");
            }
        }

    }

    private URL resolveURL(URL base, String relativeUrl) throws MalformedURLException {
        if (relativeUrl.startsWith("http")) {
            return new URL(relativeUrl);
        } else {
            String protocol = base.getProtocol();
            String host = base.getHost();
            Path bp = Paths.get(base.getPath());
            Path resolved = bp.getParent().resolve(relativeUrl).normalize();
            return new URL(protocol, host, resolved.toString());
        }
    }

    long download(URL url, Path path) throws IOException {
        InputStream inputStream = url.openStream();
        long size = Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        return size;
    }
}
