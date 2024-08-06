package ru.ekuzmichev
package consumer
import product.ProductFetcher
import schedule.Cron4sNextTimeProvider.fromCronString
import schedule.ZioScheduler
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
    ZIO
      .fromTry(fromCronString(cron))
      .logged("create cron4s time provider", printResult = _.info)
      .flatMap {
        zioScheduler
          .schedule(
            productStore
              .readSourceState(sourceId)
              .map(_.fold(Seq.empty[Product])(_.products))
              .flatMap { products =>
                sendTextMessage(
                  sourceId.chatId,
                  s"Here are your watched products:\n\n" +
                    s"${products.zipWithIndex
                        .map { case (product, index) =>
                          s"${index + 1}) ${product.id}\n\t${product.priceThreshold} ₽"
                        }
                        .mkString("\n")}"
                )
              },
            _,
            s"${sourceId.userName}|${sourceId.chatId}"
          )
          .forkDaemon
          .tap(productStore.addScheduleFiberRuntime(sourceId, _))
      }
      .ignore
