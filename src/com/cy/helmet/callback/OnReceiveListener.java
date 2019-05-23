package com.cy.helmet.callback;

import com.cy.helmet.core.protocol.ServerHelmet;

/**
 * Created by yaojiaqing on 2017/12/30.
 */

public interface OnReceiveListener {
    void handleReceiveMessage(ServerHelmet.S2HMessage msg);
}
