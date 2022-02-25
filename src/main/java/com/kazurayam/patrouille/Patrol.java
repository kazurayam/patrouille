package com.kazurayam.patrouille;

import com.kazurayam.materialstore.filesystem.Store;
import com.kazurayam.materialstore.filesystem.Stores;
import com.kazurayam.patrouille.missions.TsumitateNISA;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Patrol {

    private final Store store;

    public Patrol(Store store) {
        Objects.requireNonNull(store);
        this.store = store;
    }

    public void carryoutAll(List<Mission> missions) throws IOException {
        WebDriverManager.chromedriver().setup();
        WebDriver browser = new ChromeDriver();
        for (Mission mission : missions) {
            mission.carryout(browser, store);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            usage();
            System.exit(1);
        }
        Path root = Paths.get(args[0]);
        Store store = Stores.newInstance(root);
        WebDriver browser = new ChromeDriver();
        Patrol patrol = new Patrol(store);
        List<Mission> missions = Collections.singletonList(new TsumitateNISA());
        patrol.carryoutAll(missions);
    }

    private static void usage() {
        System.err.println("Usage: java Patrol <Path of store> <jobName>");
    }
}
