/*******************************************************************************
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.dkpro.tc.features.ngram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ExternalResourceDescription;
import org.dkpro.tc.api.features.FeatureStore;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.io.JsonDataWriter;
import org.dkpro.tc.core.util.TaskUtils;
import org.dkpro.tc.features.ngram.io.TestReaderSingleLabel;
import org.dkpro.tc.features.ngram.meta.KeywordNGramMetaCollector;
import org.dkpro.tc.features.ngram.util.KeywordNGramUtils;
import org.dkpro.tc.fstore.simple.DenseFeatureStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.Gson;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class KeywordNGramFeatureExtractorTest
{

    FeatureStore fs;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setupLogging()
    {
        System.setProperty("org.apache.uima.logger.class",
                "org.apache.uima.util.impl.Log4jLogger_impl");
    }

    private void initialize(boolean includeComma, boolean markSentenceLocation)
        throws Exception
    {

        File luceneFolder = folder.newFolder();
        File outputPath = folder.newFolder();

        Object[] parameters = new Object[] {KeywordNGram.PARAM_UNIQUE_EXTRACTOR_NAME,"123",  
                KeywordNGram.PARAM_NGRAM_KEYWORDS_FILE,
                "src/test/resources/data/keywordlist.txt", KeywordNGram.PARAM_SOURCE_LOCATION,
                luceneFolder,KeywordNGramMetaCollector.PARAM_TARGET_LOCATION,
                luceneFolder, KeywordNGram.PARAM_KEYWORD_NGRAM_MARK_SENTENCE_LOCATION,
                markSentenceLocation, KeywordNGram.PARAM_KEYWORD_NGRAM_INCLUDE_COMMAS,
                includeComma };

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TestReaderSingleLabel.class, TestReaderSingleLabel.PARAM_SOURCE_LOCATION,
                "src/test/resources/ngrams/trees.txt");

        AnalysisEngineDescription segmenter = AnalysisEngineFactory
                .createEngineDescription(BreakIteratorSegmenter.class);

        AnalysisEngineDescription metaCollector = AnalysisEngineFactory
                .createEngineDescription(KeywordNGramMetaCollector.class, parameters);

        ExternalResourceDescription featureExtractor = ExternalResourceFactory
                .createExternalResourceDescription(KeywordNGram.class, toString(parameters));
        List<ExternalResourceDescription> fes = new ArrayList<>();
        fes.add(featureExtractor);
        
        AnalysisEngineDescription featExtractorConnector = TaskUtils.getFeatureExtractorConnector(
                outputPath.getAbsolutePath(), JsonDataWriter.class.getName(),
                Constants.LM_SINGLE_LABEL, Constants.FM_DOCUMENT, DenseFeatureStore.class.getName(),
                false, false, false, new ArrayList<>(), false, fes);

        // run meta collector
        SimplePipeline.runPipeline(reader, segmenter, metaCollector);

        // run FE(s)
        SimplePipeline.runPipeline(reader, segmenter, featExtractorConnector);

        Gson gson = new Gson();
        fs = gson.fromJson(
                FileUtils.readFileToString(new File(outputPath, JsonDataWriter.JSON_FILE_NAME)),
                DenseFeatureStore.class);
        assertEquals(1, fs.getNumberOfInstances());
    }

    private Object [] toString(Object[] parameters)
    {
        List<Object> out = new ArrayList<>();
        for(Object o : parameters){
            out.add(o.toString());
        }
        
        return out.toArray();
    }

    @Test
    public void extractKeywordsTest()
        throws Exception
    {
        initialize(false, false);

        assertTrue(fs.getFeatureNames().contains("keyNG_cherry"));
        assertTrue(fs.getFeatureNames().contains("keyNG_apricot_peach"));
        assertTrue(fs.getFeatureNames().contains("keyNG_peach_nectarine_SB"));
        assertTrue(fs.getFeatureNames()
                .contains("keyNG_cherry" + KeywordNGramUtils.MIDNGRAMGLUE + "trees"));

        assertFalse(fs.getFeatureNames().contains("keyNG_guava"));
        assertFalse(fs.getFeatureNames().contains("keyNG_peach_CA"));
        assertFalse(fs.getFeatureNames().contains("keyNG_nectarine_SBBEG"));
    }

    @Test
    public void commasTest()
        throws Exception
    {
        initialize(true, false);

        assertTrue(fs.getFeatureNames().contains("keyNG_cherry"));
        assertFalse(fs.getFeatureNames().contains("keyNG_apricot_peach"));
        assertFalse(fs.getFeatureNames().contains("keyNG_peach_nectarine_SB"));
        assertTrue(fs.getFeatureNames()
                .contains("keyNG_cherry" + KeywordNGramUtils.MIDNGRAMGLUE + "trees"));

        assertFalse(fs.getFeatureNames().contains("keyNG_guava"));
        assertTrue(fs.getFeatureNames().contains("keyNG_peach_CA"));
        assertFalse(fs.getFeatureNames().contains("keyNG_nectarine_SBBEG"));

    }

    @Test
    public void sentenceLocationTest()
        throws Exception
    {
        initialize(false, true);

        assertTrue(fs.getFeatureNames().contains("keyNG_cherry"));
        assertTrue(fs.getFeatureNames().contains("keyNG_apricot_peach"));
        assertFalse(fs.getFeatureNames().contains("keyNG_peach_nectarine_SB"));
        assertTrue(fs.getFeatureNames()
                .contains("keyNG_cherry" + KeywordNGramUtils.MIDNGRAMGLUE + "trees"));

        assertFalse(fs.getFeatureNames().contains("keyNG_guava"));
        assertFalse(fs.getFeatureNames().contains("keyNG_peach_CA"));
        assertTrue(fs.getFeatureNames().contains("keyNG_nectarine_SBBEG"));
    }

}
