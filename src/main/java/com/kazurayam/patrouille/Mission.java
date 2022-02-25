package com.kazurayam.patrouille;

import com.kazurayam.materialstore.filesystem.JobName;
import com.kazurayam.materialstore.filesystem.JobTimestamp;
import com.kazurayam.materialstore.filesystem.Store;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public interface Mission {

    JobName jobName();

    URL url() throws MalformedURLException;

    JobTimestamp carryout(WebDriver driver, Store store) throws IOException;

}
