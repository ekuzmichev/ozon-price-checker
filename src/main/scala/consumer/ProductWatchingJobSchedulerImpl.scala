package ru.ekuzmichev
package consumer
import common.ProductId
import product.{ProductFetcher, ProductInfo}
import schedule.Cron4sNextTimeProvider.fromCronString
import schedule.{NextTimeProvider, ZioScheduler}
import store.ProductStore
import store.ProductStore.{Product, SourceId, SourceState}
import util.telegram.MessageSendingUtils.sendTextMessage
import util.zio.ZioLoggingImplicits.Ops

import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{Fiber, LogAnnotation, Ref, Task, UIO, ZIO}

class ProductWatchingJobSchedulerImpl(
    telegramClient: TelegramClient,
    productStore: ProductStore,
    productFetcher: ProductFetcher,
    zioScheduler: ZioScheduler,
    cron: String,
    scheduleFiberRuntimeRef: Ref[Option[Fiber.Runtime[Any, Unit]]]
) extends ProductWatchingJobScheduler:

  private implicit val _telegramClient: TelegramClient = telegramClient

  override def scheduleProductsWatching(): Task[Unit] =
    resetPreviousSchedule *>
      initNextTimeProvider().flatMap {
        zioScheduler
          .schedule(makeProductWatchJob(), _, s"product-watching")
          .forkDaemon
          .tap(runtime => scheduleFiberRuntimeRef.set(Some(runtime)))
      }.ignore

  private def resetPreviousSchedule: UIO[Unit] =
    scheduleFiberRuntimeRef.get.flatMap {
      case Some(runtime) =>
        runtime.interrupt
          .tap(exit => ZIO.log(s"Interrupted previous schedule ${runtime.id.id}: $exit"))
          .ignore
          .zipLeft(scheduleFiberRuntimeRef.set(None))
      case None =>
        ZIO.unit
    }

  private def makeProductWatchJob(): Task[Unit] =
    productStore
      .readAll()
      .tap(sourceStatesBySourceId => ZIO.log(s"Read ${sourceStatesBySourceId.size} source ids"))
      .flatMap { (sourceStatesBySourceId: Map[SourceId, SourceState]) =>
        ZIO.foreachDiscard(sourceStatesBySourceId) { case (sourceId, sourceState) =>
          ZIO
            .logAnnotate(LogAnnotation("userName", sourceId.userName), LogAnnotation("chatId", sourceId.chatId)) {
              checkAndNotifyProductPrices(sourceId, sourceState.products)
            }
        }
      }

  private def checkAndNotifyProductPrices(sourceId: SourceId, products: Seq[Product]) = {
    fetchProductInfos(products)
      .flatMap { (productInfos: Map[ProductId, ProductInfo]) =>
        val productPriceInfosReachedThreshold = products
          .map(product =>
            ProductPriceInfo(
              id = product.id,
              currentPrice = productInfos.get(product.id).map(_.price).getOrElse(Double.NaN),
              priceThreshold = product.priceThreshold
            )
          )
          .filter(productPriceInfo =>
            productInfos.get(productPriceInfo.id).exists(_.price <= productPriceInfo.priceThreshold)
          )

        ZIO.when(products.isEmpty)(ZIO.log(s"Got 0 products")) *>
          ZIO.when(products.nonEmpty)(
            ZIO.log(s"Got ${productPriceInfosReachedThreshold.size}/${products.size} products reached threshold")
          ) *>
          notifyProductsReachedThreshold(sourceId, productPriceInfosReachedThreshold)
      }
  }

  private case class ProductPriceInfo(id: ProductId, currentPrice: Double, priceThreshold: Double)

  private def notifyProductsReachedThreshold(sourceId: SourceId, productPriceInfos: Seq[ProductPriceInfo]) =
    ZIO.when(productPriceInfos.nonEmpty)(
      sendTextMessage(
        sourceId.chatId,
        s"Here are your products that have reached price threshold:\n\n" +
          s"${productPriceInfos.zipWithIndex
              .map { case (productPriceInfo, index) =>
                s"${index + 1}) ${productPriceInfo.id}\n\t" +
                  s"Current price: ${productPriceInfo.currentPrice} ₽\n\t" +
                  s"Price threshold: ${productPriceInfo.priceThreshold} ₽"
              }
              .mkString("\n")}"
      )
    )

  private def fetchProductInfos(products: Seq[Product]): Task[Map[ProductId, ProductInfo]] =
    ZIO
      .foreach(products)(product => ZIO.attempt(product.id -> productFetcher.fetchProductInfo(product.id)))
      .map(_.toMap)

  private def initNextTimeProvider(): ZIO[Any, Throwable, NextTimeProvider] =
    ZIO
      .fromTry(fromCronString(cron))
      .logged("create cron4s time provider", printResult = _.info)
