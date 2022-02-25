package com.kazurayam.patrouille.missions;

import com.kazurayam.patrouille.Mission;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public abstract class AbstractMission implements Mission {

    protected URL resolveURL(URL base, String relativeUrl) {
        try {
            if (relativeUrl.startsWith("http")) {
                return new URL(relativeUrl);
            } else {
                String protocol = base.getProtocol();
                String host = base.getHost();
                Path bp = Paths.get(base.getPath());
                Path resolved = bp.getParent().resolve(relativeUrl).normalize();
                return new URL(protocol, host, resolved.toString());
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected long download(URL url, Path path) throws IOException {
        InputStream inputStream = url.openStream();
        long size = Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        return size;
    }

}
