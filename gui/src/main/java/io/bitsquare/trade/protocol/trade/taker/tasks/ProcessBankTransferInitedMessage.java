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

import io.bitsquare.trade.protocol.trade.offerer.messages.BankTransferInitedMessage;
import io.bitsquare.trade.protocol.trade.taker.SellerAsTakerModel;
import io.bitsquare.util.tasks.Task;
import io.bitsquare.util.tasks.TaskRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.bitsquare.util.Validator.*;

public class ProcessBankTransferInitedMessage extends Task<SellerAsTakerModel> {
    private static final Logger log = LoggerFactory.getLogger(ProcessBankTransferInitedMessage.class);

    public ProcessBankTransferInitedMessage(TaskRunner taskHandler, SellerAsTakerModel model) {
        super(taskHandler, model);
    }

    @Override
    protected void run() {
        try {
            checkTradeId(model.getTrade().getId(), model.getTradeMessage());
            BankTransferInitedMessage message = (BankTransferInitedMessage) model.getTradeMessage();
            
            model.setDepositTxAsHex(nonEmptyStringOf(message.getDepositTxAsHex()));
            model.setOffererSignatureR(nonEmptyStringOf(message.getOffererSignatureR()));
            model.setOffererSignatureS(nonEmptyStringOf(message.getOffererSignatureS()));
            model.setOffererPaybackAmount(positiveCoinOf(nonZeroCoinOf(message.getOffererPaybackAmount())));
            model.setTakerPaybackAmount(positiveCoinOf(nonZeroCoinOf(message.getTakerPaybackAmount())));
            model.setOffererPayoutAddress(nonEmptyStringOf(message.getOffererPayoutAddress()));

            // TODO  listener.onBankTransferInited(message.getTrade().getId());
            complete();
        } catch (Throwable t) {
            failed(t);
        }
    }
}