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
package org.apache.pinot.broker.broker;

import org.apache.helix.NotificationContext;
import org.apache.helix.model.Message;
import org.apache.helix.participant.statemachine.StateModel;
import org.apache.helix.participant.statemachine.StateModelFactory;
import org.apache.helix.participant.statemachine.StateModelInfo;
import org.apache.helix.participant.statemachine.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EmptySegmentOnlineOfflineStateModelFactory extends StateModelFactory<StateModel> {

  @Override
  public StateModel createNewStateModel(String partitionName) {
    final EmptySegmentOnlineOfflineStateModel SegmentOnlineOfflineStateModel =
        new EmptySegmentOnlineOfflineStateModel();
    return SegmentOnlineOfflineStateModel;
  }

  public EmptySegmentOnlineOfflineStateModelFactory() {
  }

  public static String getStateModelDef() {
    return "SegmentOnlineOfflineStateModel";
  }

  @StateModelInfo(states = "{'OFFLINE','ONLINE', 'DROPPED'}", initialState = "OFFLINE")
  public static class EmptySegmentOnlineOfflineStateModel extends StateModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmptySegmentOnlineOfflineStateModel.class);

    @Transition(from = "OFFLINE", to = "ONLINE")
    public void onBecomeOnlineFromOffline(Message message, NotificationContext context) {
      LOGGER.debug("EmptySegmentOnlineOfflineStateModel.onBecomeOnlineFromOffline() : {}", message);
    }

    @Transition(from = "ONLINE", to = "OFFLINE")
    public void onBecomeOfflineFromOnline(Message message, NotificationContext context) {
      LOGGER.debug("EmptySegmentOnlineOfflineStateModel.onBecomeOfflineFromOnline() : {}", message);
    }

    @Transition(from = "OFFLINE", to = "DROPPED")
    public void onBecomeDroppedFromOffline(Message message, NotificationContext context) {
      LOGGER.debug("EmptySegmentOnlineOfflineStateModel.onBecomeDroppedFromOffline() : {}", message);
    }

    @Transition(from = "ONLINE", to = "DROPPED")
    public void onBecomeDroppedFromOnline(Message message, NotificationContext context) {
      LOGGER.debug("EmptySegmentOnlineOfflineStateModel.onBecomeDroppedFromOnline() : {}", message);
    }
  }
}
