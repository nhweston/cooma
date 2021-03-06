/*
 * This file is part of Cooma.
 *
 * Copyright (C) 2019-2021 Anthony M Sloane, Macquarie University.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.bitbucket.inkytonik.cooma.truffle.exceptions;

import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.nodes.Node;

public class CoomaException extends RuntimeException implements TruffleException {

    private static final long serialVersionUID = 1L;

    private final Node location;

    public CoomaException(String message, Node location) {
        super(message);
        this.location = location;
    }

    public CoomaException(Throwable throwable, Node location) {
        super(throwable);
        this.location = location;
    }

    @Override
    public Node getLocation() {
        return location;
    }
}
