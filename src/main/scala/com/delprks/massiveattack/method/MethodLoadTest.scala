package com.delprks.massiveattack.method

import java.util.concurrent.Executors

import com.delprks.massiveattack.method.util.{MethodOps, ResultOps}

import scala.collection.mutable.ListBuffer
import scala.collection.parallel.ForkJoinTaskSupport
import scala.collection.parallel.mutable.ParArray
import com.twitter.util.{Future => TwitterFuture}

import scala.concurrent.{ExecutionContext, Future => ScalaFuture}

class MethodLoadTest(props: MassiveAttackProperties = MassiveAttackProperties()) {

  private implicit val context: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(props.threads))
  private val methodOps = new MethodOps()
  private val resultOps = new ResultOps()

  def test(longRunningMethod: () => TwitterFuture[_]): TwitterFuture[MassiveAttackResult] = {
    if (props.warmUp) {
      println(s"Warming up the JVM...")

      methodOps.warmUpMethod(longRunningMethod, props.warmUpInvocations)
    }

    println(s"Invoking long running method ${props.invocations} times - or maximum ${props.duration} seconds")

    val testStartTime = System.currentTimeMillis()
    val testEndTime = testStartTime + props.duration * 1000

    val parallelInvocation: ParArray[Int] = (1 to props.invocations).toParArray
    parallelInvocation.tasksupport = new ForkJoinTaskSupport(new java.util.concurrent.ForkJoinPool(props.threads))

    val results: ListBuffer[TwitterFuture[MeasureResult]] = methodOps.measure(parallelInvocation, () => longRunningMethod(), testEndTime)

    val testDuration: Double = (System.currentTimeMillis() - testStartTime).toDouble
    val testResultsF: TwitterFuture[MassiveAttackResult] = resultOps.twitterTestResults(results)

    println(s"Test finished. Duration: ${testDuration / 1000}s")

    testResultsF
  }

  def test(longRunningMethod: () => ScalaFuture[_]): ScalaFuture[MassiveAttackResult] = {
    if (props.warmUp) {
      println(s"Warming up the JVM...")

      methodOps.warmUpMethod(longRunningMethod, props.warmUpInvocations)
    }

    println(s"Invoking long running method ${props.invocations} times - or maximum ${props.duration} seconds")

    val testStartTime = System.currentTimeMillis()
    val testEndTime = testStartTime + props.duration * 1000
    val parallelInvocation: ParArray[Int] = (1 to props.invocations).toParArray

    parallelInvocation.tasksupport = new ForkJoinTaskSupport(new java.util.concurrent.ForkJoinPool(props.threads))

    val results: ListBuffer[ScalaFuture[MeasureResult]] = methodOps.measure(parallelInvocation, () => longRunningMethod(), testEndTime)

    val testDuration: Long = System.currentTimeMillis() - testStartTime
    val testResultsF: ScalaFuture[MassiveAttackResult] = resultOps.scalaTestResults(results)

    println(s"Test finished. Duration: ${testDuration / 1000}s")

    testResultsF
  }

}
