package com.kazurayam.patrouille.missions;

import com.kazurayam.materialstore.filesystem.Store;
import com.kazurayam.materialstore.filesystem.Stores;
import com.kazurayam.patrouille.Mission;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TsumitateNISATest {

    private static Store store;

    @BeforeAll
    public static void beforeAll() throws IOException {
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        Path outputDir = projectDir.resolve("build/tmp/testOutput")
                .resolve(TsumitateNISATest.class.getName());
        Files.createDirectories(outputDir);
        Path root = outputDir.resolve("store");
        store = Stores.newInstance(root);
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setup() {}

    @Test
    public void test_smoke() throws IOException {
        WebDriver browser = new ChromeDriver();
        Mission mission = new TsumitateNISA();
        mission.carryout(browser, store);
    }
}

