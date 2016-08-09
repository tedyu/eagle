/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.eagle.alert.engine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.utils.DateTimeUtil;

/**
 * @since Apr 5, 2016
 *
 */
public class StreamEvent implements Serializable {
    private static final long serialVersionUID = 2765116509856609763L;

    private String streamId;
    private Object[] data;
    private long timestamp;

    public StreamEvent(){}

    public StreamEvent(String streamId,long timestamp,Object[] data){
        this.setStreamId(streamId);
        this.setTimestamp(timestamp);
        this.setData(data);
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public Object[] getData() {
        return data;
    }

    public void setData(Object[] data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(streamId).append(timestamp).append(data).build();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(obj instanceof StreamEvent){
            StreamEvent another = (StreamEvent) obj;
            return Objects.equals(this.streamId,another.streamId) && this.timestamp == another.timestamp && Arrays.deepEquals(this.data,another.data);
        }
        return false;
    }

    @Override
    public String toString() {
        List<String> dataStrings = new ArrayList<>();
        if(this.getData() != null) {
            for (Object obj : this.getData()) {
                if (obj != null) {
                    dataStrings.add(obj.toString());
                } else {
                    dataStrings.add(null);
                }
            }
        }
        return String.format("StreamEvent[stream=%S,timestamp=%s,data=[%s]]",this.getStreamId(), DateTimeUtil.millisecondsToHumanDateWithMilliseconds(this.getTimestamp()), StringUtils.join(dataStrings,","));
    }

    public static StreamEventBuilder Builder(){
        return new StreamEventBuilder();
    }

    /**
     * @return cloned new event object
     */
    public StreamEvent copy(){
        StreamEvent newEvent = new StreamEvent();
        newEvent.setTimestamp(this.getTimestamp());
        newEvent.setData(this.getData());
        newEvent.setStreamId(this.getStreamId());
        return newEvent;
    }

    public void copyFrom(StreamEvent event){
        this.setTimestamp(event.getTimestamp());
        this.setData(event.getData());
        this.setStreamId(event.getStreamId());
    }

    /**
     * @param column
     * @param streamDefinition
     * @return
     */
    public Object[] getData(StreamDefinition streamDefinition,List<String> column) {
        ArrayList<Object> result = new ArrayList<>(column.size());
        for (String colName : column) {
            result.add(this.getData()[streamDefinition.getColumnIndex(colName)]);
        }
        return result.toArray();
    }

    public Object[] getData(StreamDefinition streamDefinition,String ... column) {
        ArrayList<Object> result = new ArrayList<>(column.length);
        for (String colName : column) {
            result.add(this.getData()[streamDefinition.getColumnIndex(colName)]);
        }
        return result.toArray();
    }
}