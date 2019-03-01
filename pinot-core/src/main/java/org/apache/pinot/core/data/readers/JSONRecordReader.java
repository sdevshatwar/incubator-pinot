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
package org.apache.pinot.core.data.readers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import org.apache.pinot.common.data.FieldSpec;
import org.apache.pinot.common.data.Schema;
import org.apache.pinot.common.utils.JsonUtils;
import org.apache.pinot.core.data.GenericRow;
import org.apache.pinot.core.indexsegment.generator.SegmentGeneratorConfig;


/**
 * Record reader for JSON file.
 */
public class JSONRecordReader implements RecordReader {
  private final JsonFactory _factory = new JsonFactory();
  private final File _dataFile;
  private final Schema _schema;

  private JsonParser _parser;
  private Iterator<Map> _iterator;


  public JSONRecordReader(File dataFile, Schema schema)
      throws IOException {
    _dataFile = dataFile;
    _schema = schema;

    initialize();
  }

  private void initialize()
      throws IOException {
    _parser = _factory.createParser(RecordReaderUtils.getFileReader(_dataFile));
    try {
      _iterator = JsonUtils.DEFAULT_MAPPER.readValues(_parser, Map.class);
    } catch (Exception e) {
      _parser.close();
      throw e;
    }
  }

  @Override
  public void init(SegmentGeneratorConfig segmentGeneratorConfig) {

  }

  @Override
  public boolean hasNext() {
    return _iterator.hasNext();
  }

  @Override
  public GenericRow next() {
    return next(new GenericRow());
  }

  @Override
  public GenericRow next(GenericRow reuse) {
    Map record = _iterator.next();

    for (FieldSpec fieldSpec : _schema.getAllFieldSpecs()) {
      String fieldName = fieldSpec.getName();
      Object jsonValue = record.get(fieldName);

      Object value;
      if (fieldSpec.isSingleValueField()) {
        String token = jsonValue != null ? jsonValue.toString() : null;
        value = RecordReaderUtils.convertToDataType(token, fieldSpec);
      } else {
        value = RecordReaderUtils.convertToDataTypeArray((ArrayList) jsonValue, fieldSpec);
      }

      reuse.putField(fieldName, value);
    }

    return reuse;
  }

  @Override
  public void rewind()
      throws IOException {
    _parser.close();
    initialize();
  }

  @Override
  public Schema getSchema() {
    return _schema;
  }

  @Override
  public void close()
      throws IOException {
    _parser.close();
  }
}
