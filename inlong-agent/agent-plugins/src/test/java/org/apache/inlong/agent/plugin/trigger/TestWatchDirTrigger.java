/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.plugin.trigger;

import static org.awaitility.Awaitility.await;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constants.AgentConstants;
import org.apache.inlong.agent.plugin.AgentBaseTestsHelper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestWatchDirTrigger {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestWatchDirTrigger.class);
    private static Path testRootDir;
    private static DirectoryTrigger dirTrigger;
    private static AgentBaseTestsHelper helper;

    @BeforeClass
    public static void setup() throws Exception {
        helper = new AgentBaseTestsHelper(TestWatchDirTrigger.class.getName()).setupAgentHome();
        testRootDir = helper.getTestRootDir();
        LOGGER.info("test root dir is {}", testRootDir);
        dirTrigger = new DirectoryTrigger();
        TriggerProfile jobConf = TriggerProfile.parseJsonStr("");
        jobConf.setInt(AgentConstants.TRIGGER_CHECK_INTERVAL, 1);
        dirTrigger.init(jobConf);
        dirTrigger.start();
    }

    @AfterClass
    public static void teardown() throws Exception {
        LOGGER.info("start to teardown test case");
        dirTrigger.stop();
        dirTrigger.join();
        helper.teardownAgentHome();
    }

    @Test
    public void testWatchDirectory() throws Exception {
        final String pathPattern = String.format("%s/\\d{1}/\\d{1}", testRootDir);
        dirTrigger.register(pathPattern);
        for (int i = 0; i < 10; i++) {
            FileUtils.forceMkdir(Paths.get(testRootDir.toString(), String.valueOf(i)).toFile());
        }
        String[] subDirs = {"a", "b", "c", "d", "e"};
        for (String subDir : subDirs) {
            FileUtils.forceMkdir(Paths.get(testRootDir.toString(), subDir).toFile());
        }
        ConcurrentHashMap<PathPattern, List<WatchKey>> allWatchers = dirTrigger.getAllWatchers();

        await().atMost(20, TimeUnit.SECONDS).until(() -> getValueSize(allWatchers) == 11);
        Assert.assertEquals(1, allWatchers.keySet().size());
        Assert.assertEquals(11, getValueSize(allWatchers));

        for (int i = 0; i < 10; i++) {
            Path currentPath = Paths.get(testRootDir.toString(), "0", String.valueOf(i));
            FileUtils.forceMkdir(currentPath.toFile());
        }

        for (String subDir : subDirs) {
            Path currentPath = Paths.get(testRootDir.toString(), "a", subDir);
            FileUtils.forceMkdir(currentPath.toFile());
        }

        await().atMost(20, TimeUnit.SECONDS).until(() -> getValueSize(allWatchers) == 21);
        Assert.assertEquals(21, getValueSize(allWatchers));
        Assert.assertEquals(1, allWatchers.keySet().size());
        dirTrigger.unregister(pathPattern);
        await().atMost(20, TimeUnit.SECONDS).until(() -> getValueSize(allWatchers) == 0);
    }

    private int getValueSize(ConcurrentHashMap<PathPattern, List<WatchKey>> allWatchers) {
        int size = 0;
        for (List<WatchKey> watchKeyList : allWatchers.values()) {
            size += watchKeyList.size();
        }
        return size;
    }

    @Test
    public void testBenchMarkForWatchHugeDirs() throws Exception {
        FileUtils.deleteDirectory(testRootDir.toFile());
        FileUtils.forceMkdir(testRootDir.toFile());
        final String pathPattern = String.format("%s/\\d{2}/[a-z]{1}/1.txt", testRootDir);
        final String[] subDirs = {"a", "b", "c", "d", "e"};
        dirTrigger.register(pathPattern);
        final ConcurrentHashMap<PathPattern, List<WatchKey>> allWatchers =
                dirTrigger.getAllWatchers();

        for (int i = 0; i < 100; i++) {
            FileUtils.forceMkdir(Paths.get(testRootDir.toString(), String.valueOf(i)).toFile());
        }

        await().atMost(20, TimeUnit.SECONDS).until(() -> getValueSize(allWatchers) == 91);
        Assert.assertEquals(91, getValueSize(allWatchers));

        for (int i = 0; i < 100; i++) {
            for (String subDir : subDirs) {
                Path currentPath = Paths.get(testRootDir.toString(), String.valueOf(i), subDir);
                FileUtils.forceMkdir(currentPath.toFile());
            }
        }
        await().atMost(20, TimeUnit.SECONDS).until(() -> getValueSize(allWatchers) == 541);
        Assert.assertEquals(541, getValueSize(allWatchers));
    }

    @Test
    public void testWatchEntity() throws Exception {
        PathPattern a1 = new PathPattern(helper.getParentPath().toString());
        PathPattern a2 = new PathPattern(helper.getParentPath().toString());
        HashMap<PathPattern, Integer> map = new HashMap<>();
        map.put(a1, 10);
        Integer result = map.remove(a2);
        Assert.assertEquals(a1, a2);
        Assert.assertEquals(10, result.intValue());
    }
}
