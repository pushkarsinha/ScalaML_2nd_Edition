/**
 * Copyright (c) 2013-2017  Patrick Nicolas - Scala for Machine Learning - All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * The source code in this file is provided by the author for the sole purpose of illustrating the
 * concepts and algorithms presented in "Scala for Machine Learning 2nd edition".
 * ISBN: 978-1-783355-874-2 Packt Publishing.
 *
 * Version 0.99.2
 */
package org.scalaml.validation

import scala.collection._
import org.scalaml.Predef._
import MulticlassValidation._
import org.scalaml.Predef.Context._

trait ArrayToInt[T] {
  def apply(t: Array[T]): Int
}
/**
 * Immutable class that implements the metrics to validate a model with multi classes
 * (or binomial F validation). The validation is applied to a test or validation run
 * The counters for TP, TN, FP and FN are computed during instantiation
 * to the class,precision and recall are computed at run-time (lazy values).
 * {{{
 *    precision (class i) = TP(i)/(TP(i) + FP(i))
 *    recall (class i) TP(i)/(TP(i) + FN(i))
 *    precision model = SUM { TP(i)/(TP(i) + FP(i)) } / num classes
 *    recall model = SUM { TP(i)/(TP(i) + FN(i)) } / num classes
 *    F1 = 2.precision.recall/(precision + recall)
 * }}}
 *
 * @constructor Create a class validation instance that compute precision, recall and F1 measure
 * @throws IllegalArgumentException if actualExpected is undefined or has no elements or
 * tpClass is out of range
 * @param labeled  Labeled data generated by zipping a vector of observations with the
 * vector of expected class (or outcome)
 * @param classes number of classes of the model
 * @author Patrick Nicolas
 * @since 0.99 June 8, 2015
 * @version 0.99.2
 * @see Scala for Machine Learning Chapter 2 "Hello World!" / Assessing a model / Validation
 * @note The companion class '''MultiFValidation''' has several versions of the constructors
 */
@throws(classOf[IllegalArgumentException])
final private[scalaml] class MulticlassValidation[T: ToDouble] protected (
    val labeled: Vector[(Array[T], Int)],
    classes: Int
)(implicit predict: Array[T] => Int) extends Validation {
  require(classes > 1, s"MultiFValidation found classes = $classes required > 1")

  /**
   * Confusion matrix of predicted versus expected values
   */
  val confusionMatrix: IMatrix = {
    val matrix = Array.fill(classes)(Array.fill(classes)(0))
    labeled./:(matrix) { case (m, (x, n)) => m(n)(predict(x)) = 1; m }
  }

  private val macroStats: DblPair = {
    val pr = Range(0, classes)./:(0.0, 0.0)((s, n) => {
      val tp = confusionMatrix(n)(n)
      val fn = col(n, confusionMatrix).sum - tp
      val fp = confusionMatrix(n).sum - tp
      (s._1 + tp.toDouble / (tp + fp), s._2 + tp.toDouble / (tp + fn))
    })
    (pr._1 / classes, pr._2 / classes)
  }

  /*
		 * Compute the precision of the model - classifier using the macro formula
		 */
  lazy val precision: Double = macroStats._1

  /*
		 * Compute the recall of the model - classifier using the macro formula
		 */
  lazy val recall: Double = macroStats._1

  /*
		 * Compute the F1 measure for a model - classifier using the macro formula
		 */
  override def score: Double = 2.0 * precision * recall / (precision + recall)

  override def toString: String = s"confusion Matrix: ${confusionMatrix.toString}"
}

/**
 * MultiFValidation companion singleton used to define a variety of constructors for the class
 *
 * @author Patrick Nicolas
 * @version 0.99.1
 */
private[scalaml] object MulticlassValidation {
  type IMatrix = Array[Array[Int]]

  /**
   * Default constructor for the F1 scoring validation of a multi dimensional observations
   *
   * @param labeled  Labeled data generated by zipping a vector of observations with the
   * vector of expected class (or outcome)
   * @param classes number of classes of the model
   */
  def apply[T: ToDouble](
    labeled: Vector[(Array[T], Int)],
    classes: Int
  )(implicit predict: Array[T] => Int) = new MulticlassValidation[T](labeled, classes)

  /**
   * Default constructor for the F1 scoring validation of the binomial observations
   *
   * @param labeled  Labeled data generated by zipping a vector of observations with the
   * vector of expected class (or outcome)
   */
  def apply[T: ToDouble](
    labeled: Vector[(Array[T], Int)]
  )(implicit predict: Array[T] => Int) = new MulticlassValidation[T](labeled, 2)

  /**
   * Constructor for the F1 scoring validation of a multi dimensional observations
   * @param expected Vector of expected classes used in training
   * @param xv time series or set of observations used for validating the model
   * @param classes number of classes of the model
   */
  def apply[T: ToDouble](
    expected: Vector[Int],
    xv: Vector[Array[T]],
    classes: Int
  )(implicit predict: Array[T] => Int) = new MulticlassValidation[T](xv.zip(expected), classes)

  def col(n: Int, m: IMatrix): Array[Int] =
    m.head.indices./:(mutable.ArrayBuffer[Int]())((b, j) => b += m(j)(n)).toArray
}

// --------------------  EOF --------------------------------------------------------