package ru.ekuzmichev
package consumer
import common.ProductId
import product.{ProductFetcher, ProductInfo}
import schedule.Cron4sNextTimeProvider.fromCronString
import schedule.{NextTimeProvider, ZioScheduler}
import store.ProductStore
import store.ProductStore.{Product, SourceId}
import util.telegram.MessageSendingUtils.sendTextMessage
import util.zio.ZioLoggingImplicits.Ops

import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{Task, ZIO}

class ProductWatchingJobSchedulerImpl(
    telegramClient: TelegramClient,
    productStore: ProductStore,
    productFetcher: ProductFetcher,
    zioScheduler: ZioScheduler,
    cron: String
) extends ProductWatchingJobScheduler:

  private implicit val _telegramClient: TelegramClient = telegramClient

  override def scheduleProductsWatching(sourceId: ProductStore.SourceId): Task[Unit] =
    initNextTimeProvider().flatMap {
      zioScheduler
        .schedule(makeProductWatchJob(sourceId), _, s"${sourceId.userName}|${sourceId.chatId}")
        .forkDaemon
        .tap(productStore.addScheduleFiberRuntime(sourceId, _))
    }.ignore

  private def makeProductWatchJob(sourceId: SourceId) =
    productStore
      .readSourceState(sourceId)
      .map(_.fold(Seq.empty[Product])(_.products))
      .flatMap { products =>
        fetchProductInfos(products)
          .flatMap { productInfos =>
            val productsReachedThreshold: Seq[Product] =
              products
                .filter(product => productInfos.get(product.id).exists(_.price <= product.priceThreshold))
            ZIO.when(productsReachedThreshold.nonEmpty)(
              sendTextMessage(
                sourceId.chatId,
                s"Here are your products that have reached price threshold:\n\n" +
                  s"${productsReachedThreshold.zipWithIndex
                      .map { case (product, index) =>
                        s"${index + 1}) ${product.id}\n\t" +
                          s"Current price: ${productInfos.get(product.id).map(_.price).getOrElse(Double.NaN)} ₽\n\t" +
                          s"Price threshold: ${product.priceThreshold} ₽"
                      }
                      .mkString("\n")}"
              )
            )
          }
      }

  private def fetchProductInfos(products: Seq[Product]): Task[Map[ProductId, ProductInfo]] =
    ZIO
      .foreach(products)(product => ZIO.attempt(product.id -> productFetcher.fetchProductInfo(product.id)))
      .map(_.toMap)

  private def initNextTimeProvider(): ZIO[Any, Throwable, NextTimeProvider] =
    ZIO
      .fromTry(fromCronString(cron))
      .logged("create cron4s time provider", printResult = _.info)
