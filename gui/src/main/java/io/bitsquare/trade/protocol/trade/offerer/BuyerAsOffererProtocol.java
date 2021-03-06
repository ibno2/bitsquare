/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bitsquare.trade.protocol.trade.offerer;

import io.bitsquare.network.Message;
import io.bitsquare.network.Peer;
import io.bitsquare.trade.protocol.trade.TradeMessage;
import io.bitsquare.trade.protocol.trade.offerer.tasks.CreateDepositTx;
import io.bitsquare.trade.protocol.trade.offerer.tasks.ProcessPayoutTxPublishedMessage;
import io.bitsquare.trade.protocol.trade.offerer.tasks.ProcessRequestOffererPublishDepositTxMessage;
import io.bitsquare.trade.protocol.trade.offerer.tasks.ProcessRequestTakeOfferMessage;
import io.bitsquare.trade.protocol.trade.offerer.tasks.ProcessTakeOfferFeePayedMessage;
import io.bitsquare.trade.protocol.trade.offerer.tasks.RespondToTakeOfferRequest;
import io.bitsquare.trade.protocol.trade.offerer.tasks.SendBankTransferInitedMessage;
import io.bitsquare.trade.protocol.trade.offerer.tasks.SendDepositTxIdToTaker;
import io.bitsquare.trade.protocol.trade.offerer.tasks.SendTakerDepositPaymentRequest;
import io.bitsquare.trade.protocol.trade.offerer.tasks.SetupListenerForBlockChainConfirmation;
import io.bitsquare.trade.protocol.trade.offerer.tasks.SignAndPublishDepositTx;
import io.bitsquare.trade.protocol.trade.offerer.tasks.SignPayoutTx;
import io.bitsquare.trade.protocol.trade.offerer.tasks.VerifyAndSignContract;
import io.bitsquare.trade.protocol.trade.offerer.tasks.VerifyTakeOfferFeePayment;
import io.bitsquare.trade.protocol.trade.offerer.tasks.VerifyTakerAccount;
import io.bitsquare.trade.protocol.trade.taker.messages.PayoutTxPublishedMessage;
import io.bitsquare.trade.protocol.trade.taker.messages.RequestOffererPublishDepositTxMessage;
import io.bitsquare.trade.protocol.trade.taker.messages.RequestTakeOfferMessage;
import io.bitsquare.trade.protocol.trade.taker.messages.TakeOfferFeePayedMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.bitsquare.util.Validator.nonEmptyStringOf;

/**
 * Responsible for the correct execution of the sequence of tasks, message passing to the peer and message processing
 * from the peer.
 * <p/>
 * This class handles the role of the offerer as the Bitcoin buyer.
 * <p/>
 * It uses sub tasks to not pollute the main class too much with all the async result/fault handling.
 * Any data from incoming messages need to be validated before further processing.
 */
public class BuyerAsOffererProtocol {

    private static final Logger log = LoggerFactory.getLogger(BuyerAsOffererProtocol.class);

    private BuyerAsOffererModel model;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    public BuyerAsOffererProtocol(BuyerAsOffererModel model) {
        this.model = model;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Public methods
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void start() {
        model.getTradeMessageService().addMessageHandler(this::handleMessage);
    }

    public void cleanup() {
        model.getTradeMessageService().removeMessageHandler(this::handleMessage);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Incoming message handling
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void handleMessage(Message message, Peer peer) {
        log.trace("handleNewMessage: message = " + message.getClass().getSimpleName());
        if (message instanceof TradeMessage) {
            TradeMessage tradeMessage = (TradeMessage) message;
            nonEmptyStringOf(tradeMessage.getTradeId());

            if (tradeMessage instanceof RequestTakeOfferMessage) {
                handleRequestTakeOfferMessage((RequestTakeOfferMessage) tradeMessage, peer);
            }
            else if (tradeMessage instanceof TakeOfferFeePayedMessage) {
                handleTakeOfferFeePayedMessage((TakeOfferFeePayedMessage) tradeMessage);
            }

            else if (tradeMessage instanceof RequestOffererPublishDepositTxMessage) {
                handleRequestOffererPublishDepositTxMessage((RequestOffererPublishDepositTxMessage) tradeMessage);
            }
            else if (tradeMessage instanceof PayoutTxPublishedMessage) {
                handlePayoutTxPublishedMessage((PayoutTxPublishedMessage) tradeMessage);
            }
            else {
                log.error("Incoming tradeMessage not supported. " + tradeMessage);
            }
        }
    }

    private void handleRequestTakeOfferMessage(RequestTakeOfferMessage tradeMessage, Peer peer) {
        model.setTradeMessage(tradeMessage);
        model.setPeer(peer);

        BuyerAsOffererTaskRunner<BuyerAsOffererModel> sequence = new BuyerAsOffererTaskRunner<>(model,
                () -> {
                    log.debug("sequence0 completed");
                },
                (message, throwable) -> {
                    log.error(message);
                }
        );
        sequence.addTasks(
                ProcessRequestTakeOfferMessage.class,
                RespondToTakeOfferRequest.class
        );
        sequence.run();
    }

    private void handleTakeOfferFeePayedMessage(TakeOfferFeePayedMessage tradeMessage) {
        model.setTradeMessage(tradeMessage);

        BuyerAsOffererTaskRunner<BuyerAsOffererModel> sequence = new BuyerAsOffererTaskRunner<>(model,
                () -> {
                    log.debug("sequence1 completed");
                },
                (message, throwable) -> {
                    log.error(message);
                }
        );
        sequence.addTasks(
                ProcessTakeOfferFeePayedMessage.class,
                CreateDepositTx.class,
                SendTakerDepositPaymentRequest.class
        );
        sequence.run();
    }

    private void handleRequestOffererPublishDepositTxMessage(RequestOffererPublishDepositTxMessage tradeMessage) {
        model.setTradeMessage(tradeMessage);

        BuyerAsOffererTaskRunner<BuyerAsOffererModel> sequence = new BuyerAsOffererTaskRunner<>(model,
                () -> {
                    log.debug("sequence2 completed");
                },
                (message, throwable) -> {
                    log.error(message);
                }
        );
        sequence.addTasks(
                ProcessRequestOffererPublishDepositTxMessage.class,
                VerifyTakerAccount.class,
                VerifyAndSignContract.class,
                SignAndPublishDepositTx.class,
                SetupListenerForBlockChainConfirmation.class,
                SendDepositTxIdToTaker.class
        );
        sequence.run();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // UI event handling
    ///////////////////////////////////////////////////////////////////////////////////////////

    // User clicked the "bank transfer started" button
    public void handleBankTransferStartedUIEvent() {
        BuyerAsOffererTaskRunner<BuyerAsOffererModel> sequence = new BuyerAsOffererTaskRunner<>(model,
                () -> {
                    log.debug("sequence3 completed");
                },
                (message, throwable) -> {
                    log.error(message);
                }
        );
        sequence.addTasks(
                SignPayoutTx.class,
                VerifyTakeOfferFeePayment.class,
                SendBankTransferInitedMessage.class
        );
        sequence.run();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Incoming message handling
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void handlePayoutTxPublishedMessage(PayoutTxPublishedMessage tradeMessage) {
        model.setTradeMessage(tradeMessage);

        BuyerAsOffererTaskRunner<BuyerAsOffererModel> sequence = new BuyerAsOffererTaskRunner<>(model,
                () -> {
                    log.debug("sequence4 completed");
                },
                (message, throwable) -> {
                    log.error(message);
                }
        );
        sequence.addTasks(ProcessPayoutTxPublishedMessage.class);
        sequence.run();
    }

}
