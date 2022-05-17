/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2017 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.basedloader.maldtils.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public final class FileSystemUtil {
    private static final Map<String, String> JFS_ARGS_CREATE = Map.of("create", "true");
    private static final Map<String, String> JFS_ARGS_EMPTY = Collections.emptyMap();

    public record Delegate(FileSystem fs, boolean owner) implements AutoCloseable, Supplier<FileSystem> {
        public byte[] readAllBytes(String path) throws IOException {
            Path fsPath = get().getPath(path);

            if (Files.exists(fsPath)) {
                return Files.readAllBytes(fsPath);
            } else {
                throw new NoSuchFileException(fsPath.toString());
            }
        }

        @Override
        public void close() throws IOException {
            if (owner) {
                fs.close();
            }
        }

        @Override
        public FileSystem get() {
            return fs;
        }

    }

    private FileSystemUtil() {
    }

    public static Delegate getJarFileSystem(File file, boolean create) throws IOException {
        return getJarFileSystem(file.toURI(), create);
    }

    public static Delegate getJarFileSystem(Path path, boolean create) throws IOException {
        return getJarFileSystem(path.toUri(), create);
    }

    public static Delegate getJarFileSystem(Path path) throws IOException {
        return getJarFileSystem(path, false);
    }

    public static Delegate getJarFileSystem(URI uri, boolean create) throws IOException {
        URI jarUri;

        try {
            jarUri = new URI("jar:" + uri.getScheme(), uri.getHost(), uri.getPath(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        try {
            return new Delegate(FileSystems.newFileSystem(jarUri, create ? JFS_ARGS_CREATE : JFS_ARGS_EMPTY), true);
        } catch (ProviderNotFoundException e) {
            throw new RuntimeException("Couldn't find jar provider. Are you sure your loading a Jar?", e);
        } catch (FileSystemAlreadyExistsException e) {
            return new Delegate(FileSystems.getFileSystem(jarUri), false);
        } catch (IOException e) {
            throw new IOException("Could not create JAR file system for " + uri + " (create: " + create + ")", e);
        }
    }
}
