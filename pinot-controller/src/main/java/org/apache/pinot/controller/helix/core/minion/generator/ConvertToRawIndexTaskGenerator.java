/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.controller.helix.core.minion.generator;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.pinot.common.config.PinotTaskConfig;
import org.apache.pinot.common.config.TableConfig;
import org.apache.pinot.common.config.TableTaskConfig;
import org.apache.pinot.common.data.Segment;
import org.apache.pinot.common.metadata.segment.OfflineSegmentZKMetadata;
import org.apache.pinot.common.utils.CommonConstants;
import org.apache.pinot.controller.helix.core.minion.ClusterInfoProvider;
import org.apache.pinot.core.common.MinionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConvertToRawIndexTaskGenerator implements PinotTaskGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConvertToRawIndexTaskGenerator.class);

  private final ClusterInfoProvider _clusterInfoProvider;

  public ConvertToRawIndexTaskGenerator(ClusterInfoProvider clusterInfoProvider) {
    _clusterInfoProvider = clusterInfoProvider;
  }

  @Nonnull
  @Override
  public String getTaskType() {
    return MinionConstants.ConvertToRawIndexTask.TASK_TYPE;
  }

  @Nonnull
  @Override
  public List<PinotTaskConfig> generateTasks(@Nonnull List<TableConfig> tableConfigs) {
    List<PinotTaskConfig> pinotTaskConfigs = new ArrayList<>();

    // Get the segments that are being converted so that we don't submit them again
    Set<Segment> runningSegments =
        TaskGeneratorUtils.getRunningSegments(MinionConstants.ConvertToRawIndexTask.TASK_TYPE, _clusterInfoProvider);

    for (TableConfig tableConfig : tableConfigs) {
      // Only generate tasks for OFFLINE tables
      String offlineTableName = tableConfig.getTableName();
      if (tableConfig.getTableType() != CommonConstants.Helix.TableType.OFFLINE) {
        LOGGER.warn("Skip generating ConvertToRawIndexTask for non-OFFLINE table: {}", offlineTableName);
        continue;
      }

      TableTaskConfig tableTaskConfig = tableConfig.getTaskConfig();
      Preconditions.checkNotNull(tableTaskConfig);
      Map<String, String> taskConfigs =
          tableTaskConfig.getConfigsForTaskType(MinionConstants.ConvertToRawIndexTask.TASK_TYPE);
      Preconditions.checkNotNull(taskConfigs, "Task config shouldn't be null for Table: {}", offlineTableName);

      // Get max number of tasks for this table
      int tableMaxNumTasks;
      String tableMaxNumTasksConfig = taskConfigs.get(MinionConstants.TABLE_MAX_NUM_TASKS_KEY);
      if (tableMaxNumTasksConfig != null) {
        try {
          tableMaxNumTasks = Integer.valueOf(tableMaxNumTasksConfig);
        } catch (Exception e) {
          tableMaxNumTasks = Integer.MAX_VALUE;
        }
      } else {
        tableMaxNumTasks = Integer.MAX_VALUE;
      }

      // Get the config for columns to convert
      String columnsToConvertConfig = taskConfigs.get(MinionConstants.ConvertToRawIndexTask.COLUMNS_TO_CONVERT_KEY);

      // Generate tasks
      int tableNumTasks = 0;
      for (OfflineSegmentZKMetadata offlineSegmentZKMetadata : _clusterInfoProvider
          .getOfflineSegmentsMetadata(offlineTableName)) {
        // Generate up to tableMaxNumTasks tasks each time for each table
        if (tableNumTasks == tableMaxNumTasks) {
          break;
        }

        // Skip segments that are already submitted
        String segmentName = offlineSegmentZKMetadata.getSegmentName();
        if (runningSegments.contains(new Segment(offlineTableName, segmentName))) {
          continue;
        }

        // Only submit segments that have not been converted
        Map<String, String> customMap = offlineSegmentZKMetadata.getCustomMap();
        if (customMap == null || !customMap.containsKey(
            MinionConstants.ConvertToRawIndexTask.COLUMNS_TO_CONVERT_KEY + MinionConstants.TASK_TIME_SUFFIX)) {
          Map<String, String> configs = new HashMap<>();
          configs.put(MinionConstants.TABLE_NAME_KEY, offlineTableName);
          configs.put(MinionConstants.SEGMENT_NAME_KEY, segmentName);
          configs.put(MinionConstants.DOWNLOAD_URL_KEY, offlineSegmentZKMetadata.getDownloadUrl());
          configs.put(MinionConstants.UPLOAD_URL_KEY, _clusterInfoProvider.getVipUrl() + "/segments");
          configs.put(MinionConstants.ORIGINAL_SEGMENT_CRC_KEY, String.valueOf(offlineSegmentZKMetadata.getCrc()));
          if (columnsToConvertConfig != null) {
            configs.put(MinionConstants.ConvertToRawIndexTask.COLUMNS_TO_CONVERT_KEY, columnsToConvertConfig);
          }
          pinotTaskConfigs.add(new PinotTaskConfig(MinionConstants.ConvertToRawIndexTask.TASK_TYPE, configs));
          tableNumTasks++;
        }
      }
    }

    return pinotTaskConfigs;
  }

  @Override
  public int getNumConcurrentTasksPerInstance() {
    return DEFAULT_NUM_CONCURRENT_TASKS_PER_INSTANCE;
  }

  @Override
  public void nonLeaderCleanUp() {
  }
}
