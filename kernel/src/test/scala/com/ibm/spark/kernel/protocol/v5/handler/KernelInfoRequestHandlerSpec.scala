/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.kernel.protocol.v5.handler

import akka.actor.{ActorRef, ActorSelection, ActorSystem, Props}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import com.ibm.spark.kernel.protocol.v5.content.KernelInfoReply
import com.ibm.spark.kernel.protocol.v5.kernel.ActorLoader
import com.ibm.spark.kernel.protocol.v5.{SystemActorType, Header, KernelMessage}
import org.mockito.AdditionalMatchers.{not => mockNot}
import org.mockito.Matchers.{eq => mockEq}
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpecLike, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._

object KernelInfoRequestHandlerSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class KernelInfoRequestHandlerSpec extends TestKit(
  ActorSystem("KernelInfoRequestHandlerSpec",
    ConfigFactory.parseString(KernelInfoRequestHandlerSpec.config))
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar {
  val actorLoader: ActorLoader =  mock[ActorLoader]
  val actor = system.actorOf(Props(classOf[KernelInfoRequestHandler], actorLoader))

  val relayProbe : TestProbe = TestProbe()
  val relaySelection : ActorSelection =
    system.actorSelection(relayProbe.ref.path)
  when(actorLoader.load(SystemActorType.KernelMessageRelay))
    .thenReturn(relaySelection)
  when(actorLoader.load(mockNot(mockEq(SystemActorType.KernelMessageRelay))))
    .thenReturn(system.actorSelection(""))

  val header = Header("","","","","")
  val kernelMessage = new KernelMessage(
    Seq[String](), "test message", header, header, Map[String, String](), "{}"
  )

  describe("Kernel Info Request Handler") {
    it("should return a KernelMessage containing kernel info response") {
      actor ! kernelMessage
      val reply = relayProbe.receiveOne(1.seconds).asInstanceOf[KernelMessage]
      val kernelInfo = Json.parse(reply.contentString).as[KernelInfoReply]
      kernelInfo.implementation should be ("spark")
    }
  }
}
