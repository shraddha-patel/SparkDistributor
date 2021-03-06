/*
 * Copyright 2014 DataGenerator Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.finra.datagenerator

import java._
import java.io._
import java.util.concurrent.atomic.AtomicBoolean

import org.finra.datagenerator.consumer.{EquivalenceClassTransformer, DataPipe, DataConsumer}
import org.finra.datagenerator.distributor.multithreaded.SingleThreadedProcessing
import org.finra.datagenerator.engine.scxml.{SCXMLFrontier, SCXMLEngine}
import org.finra.datagenerator.samples.transformer.SampleMachineTransformer
import org.finra.datagenerator.writer.{DefaultWriter, DataWriter}

import org.apache.spark.{SparkConf, SparkContext}
import org.finra.datagenerator.distributor.{ProcessingStrategy, SearchDistributor}
import org.finra.datagenerator.engine.Frontier

/**
 * Take Frontiers produced by an Engine and process the Frontiers in parallel,
 * giving the results to a DataConsumer for post-processing.
 *
 * Set masterURL to SparkConf using Parametrized Constructor
 *
 * Created by Brijesh on 6/2/2015.
 */
class SparkDistributor(masterURL: String) extends
SearchDistributor with java.io.Serializable {

  //val flag: Boolean = true
  var hardExitFlag = new AtomicBoolean(false)
  var searchExitFlag = new AtomicBoolean(false)

  var threadCount: Int = 1
  var maxNumberOfLines: Long = -1

  var randomNumberQueue = new util.LinkedList[util.Map[String, String]]()

  def setMaxNumberOfLines(numberOfLines: Long): SparkDistributor = {
    this.maxNumberOfLines = numberOfLines
    this
  }

  /**
   * Set Data Consumer to consume output
   *
   * @param dataConsumer data consumer
   * @return SearchDistributor
   */
  def setDataConsumer(dataConsumer: DataConsumer): SearchDistributor = {
    dataConsumer.setExitFlag(hardExitFlag)
    this
  }

  /**
   * Sets the number of threads to use
   *
   * @param threadCount an int containing the thread count
   * @return a reference to the current DefaultDistributor
   */
  def setThreadCount(threadCount: Int): SparkDistributor = {
    this.threadCount = threadCount
    this
  }

  /**
   * Distribute the random number generated by Frontier from Frontier List
   * Process data parallel using Spark - Map, Reduce method
   *
   * Define SparkConfiguration and SparkContext,
   * Add dependency jar
   *
   * @param frontierList list of Frontiers
   */
  def distribute(frontierList: util.List[Frontier]): Unit = {

    val conf: SparkConf = new SparkConf().setMaster(masterURL).setAppName("DataGenerator")
    conf.set("spark.driver.allowMultipleContexts", "true")
    val sparkContext: SparkContext = new SparkContext(conf)

    println("Frontier List size: " + frontierList.size())
    //sparkContext.addJar("./dg-spark/target/dg-spark-2.2-SNAPSHOT.jar")

    //val frontierListArray = frontierList.toArray[frontierList]()

    //sparkContext.parallelize(frontierList).map{}

    sparkContext.parallelize(0 to (frontierList.size() - 1)).map {
      i =>

        val out: OutputStream = new FileOutputStream("./dg-spark/out/out" + Math.random() + ".txt", true)

        try {

          val dw: DefaultWriter = new DefaultWriter(out, Array[String]("var_1_1", "var_1_2", "var_1_3", "var_1_4", "var_1_5", "var_1_6",
            "var_2_1", "var_2_2", "var_2_3", "var_2_4", "var_2_5", "var_2_6"))

          val dataConsumer: DataConsumer = new DataConsumer

          dataConsumer.addDataTransformer(new SampleMachineTransformer)

          dataConsumer.addDataTransformer(new EquivalenceClassTransformer)

          dataConsumer.addDataWriter(dw)

          this.setDataConsumer(dataConsumer)

          //println("i value: " + i)

          val frontier = frontierList.get(i)

          frontier.searchForScenarios(new SingleThreadedProcessing(maxNumberOfLines), searchExitFlag)

        }

        finally {

          out.close()

        }

        0

    }.count()
  }
}




/*
    sparkContext.parallelize(0 to frontierList.size()).map {
      i=>

        val out: OutputStream = new FileOutputStream("./dg-spark/out.txt")

        val dw: DefaultWriter = new DefaultWriter(out, Array[String]("var_1_1", "var_1_2", "var_1_3", "var_1_4", "var_1_5", "var_1_6",
          "var_2_1", "var_2_2", "var_2_3", "var_2_4", "var_2_5", "var_2_6"))

        val dataConsumer: DataConsumer = new DataConsumer

        dataConsumer.addDataTransformer(new SampleMachineTransformer)

        dataConsumer.addDataTransformer(new EquivalenceClassTransformer)

        dataConsumer.addDataWriter(dw)

        this.setDataConsumer(dataConsumer)

        val frontier = frontierList.get(i)

        frontier.searchForScenarios(new SingleThreadedProcessing(dataConsumer,maxNumberOfLines), searchExitFlag)

    }
    */
