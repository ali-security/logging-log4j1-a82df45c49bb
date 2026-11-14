/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j.nt;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import org.apache.log4j.LogManager;


/**
 * Tests for the resolution of the NTEventLogAppender DLL path configured
 * through the {@link NTEventLogAppender#DLL_PATH_PROPERTY} system property.
 *
 * <p>These tests only use {@link NTEventLogAppender.DllPathResolver} and the
 * {@link NTEventLogAppender#DLL_PATH_PROPERTY} constant, so they never load
 * the native NTEventLogAppender library and can run on any platform.
 */
public class NTEventLogAppenderDllPathTest extends TestCase {

  private String originalDllPath;

  public void setUp() {
    originalDllPath = System.getProperty(NTEventLogAppender.DLL_PATH_PROPERTY);
    clearDllPathProperty();
  }

  public void tearDown() {
    if (originalDllPath != null) {
      System.setProperty(NTEventLogAppender.DLL_PATH_PROPERTY, originalDllPath);
    } else {
      clearDllPathProperty();
    }
    LogManager.shutdown();
  }

  /**
   * Removes the DLL path system property (System.clearProperty is not
   * available on JDK 1.4).
   */
  private static void clearDllPathProperty() {
    System.getProperties().remove(NTEventLogAppender.DLL_PATH_PROPERTY);
  }

  /**
   * When the system property is not set, resolveConfiguredDllPath should return null.
   */
  public void testResolveConfiguredDllPath_notSet() {
    clearDllPathProperty();
    assertNull(NTEventLogAppender.DllPathResolver.resolveConfiguredDllPath());
  }

  /**
   * When the system property is an empty string, resolveConfiguredDllPath should return null.
   */
  public void testResolveConfiguredDllPath_emptyString() {
    System.setProperty(NTEventLogAppender.DLL_PATH_PROPERTY, "");
    assertNull(NTEventLogAppender.DllPathResolver.resolveConfiguredDllPath());
  }

  /**
   * When the system property is whitespace only, resolveConfiguredDllPath should return null.
   */
  public void testResolveConfiguredDllPath_whitespaceOnly() {
    System.setProperty(NTEventLogAppender.DLL_PATH_PROPERTY, "   ");
    assertNull(NTEventLogAppender.DllPathResolver.resolveConfiguredDllPath());
  }

  /**
   * When the system property is a relative path, resolveConfiguredDllPath should return null.
   */
  public void testResolveConfiguredDllPath_relativePath() {
    System.setProperty(NTEventLogAppender.DLL_PATH_PROPERTY, "relative/path/NTEventLogAppender.dll");
    assertNull(NTEventLogAppender.DllPathResolver.resolveConfiguredDllPath());
  }

  /**
   * DLL preloading: a DLL planted in the current working directory must not be
   * picked up through a relative path, even though the file exists.
   */
  public void testResolveConfiguredDllPath_relativePathToPlantedFileInWorkingDir() throws IOException {
    File workingDir = new File(System.getProperty("user.dir"));
    File planted = File.createTempFile("NTEventLogAppender", ".dll", workingDir);
    planted.deleteOnExit();
    try {
      assertTrue(new File(planted.getName()).exists());
      System.setProperty(NTEventLogAppender.DLL_PATH_PROPERTY, planted.getName());
      assertNull(NTEventLogAppender.DllPathResolver.resolveConfiguredDllPath());
    } finally {
      planted.delete();
    }
  }

  /**
   * When the system property is an absolute path to a file that does not exist,
   * resolveConfiguredDllPath should return null.
   */
  public void testResolveConfiguredDllPath_absoluteButMissing() {
    String missingPath = new File(System.getProperty("java.io.tmpdir"), "no_such_NTEventLogAppender.dll")
        .getAbsolutePath();
    System.setProperty(NTEventLogAppender.DLL_PATH_PROPERTY, missingPath);
    assertNull(NTEventLogAppender.DllPathResolver.resolveConfiguredDllPath());
  }

  /**
   * When the system property is an absolute path to an existing file,
   * resolveConfiguredDllPath should return that absolute path.
   */
  public void testResolveConfiguredDllPath_absoluteAndExists() throws IOException {
    File tempFile = File.createTempFile("NTEventLogAppender", ".dll");
    tempFile.deleteOnExit();
    try {
      String absolutePath = tempFile.getAbsolutePath();
      System.setProperty(NTEventLogAppender.DLL_PATH_PROPERTY, absolutePath);
      String result = NTEventLogAppender.DllPathResolver.resolveConfiguredDllPath();
      assertNotNull(result);
      assertEquals(absolutePath, result);
    } finally {
      tempFile.delete();
    }
  }

  /**
   * Surrounding whitespace in the configured absolute path is ignored.
   */
  public void testResolveConfiguredDllPath_absoluteWithSurroundingWhitespace() throws IOException {
    File tempFile = File.createTempFile("NTEventLogAppender", ".dll");
    tempFile.deleteOnExit();
    try {
      String absolutePath = tempFile.getAbsolutePath();
      System.setProperty(NTEventLogAppender.DLL_PATH_PROPERTY, "  " + absolutePath + "  ");
      assertEquals(absolutePath, NTEventLogAppender.DllPathResolver.resolveConfiguredDllPath());
    } finally {
      tempFile.delete();
    }
  }
}
