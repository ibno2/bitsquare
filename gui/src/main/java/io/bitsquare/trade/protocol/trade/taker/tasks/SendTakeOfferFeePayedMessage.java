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

package io.bitsquare.trade.protocol.trade.taker.tasks;

import io.bitsquare.trade.listeners.SendMessageListener;
import io.bitsquare.trade.protocol.trade.taker.SellerAsTakerModel;
import io.bitsquare.trade.protocol.trade.taker.messages.TakeOfferFeePayedMessage;
import io.bitsquare.util.tasks.Task;
import io.bitsquare.util.tasks.TaskRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendTakeOfferFeePayedMessage extends Task<SellerAsTakerModel> {
    private static final Logger log = LoggerFactory.getLogger(SendTakeOfferFeePayedMessage.class);

    public SendTakeOfferFeePayedMessage(TaskRunner taskHandler, SellerAsTakerModel model) {
        super(taskHandler, model);
    }

    @Override
    protected void run() {
        TakeOfferFeePayedMessage msg = new TakeOfferFeePayedMessage(
                model.getTrade().getId(),
                model.getTrade().getTakeOfferFeeTxId(),
                model.getTradeAmount(),
                model.getTradePubKeyAsHex()
        );

        model.getTradeMessageService().sendMessage(model.getPeer(), msg, new SendMessageListener() {
            @Override
            public void handleResult() {
                log.trace("Sending TakeOfferFeePayedMessage succeeded.");
                complete();
            }

            @Override
            public void handleFault() {
                failed("Sending TakeOfferFeePayedMessage failed.");
            }
        });
    }
}