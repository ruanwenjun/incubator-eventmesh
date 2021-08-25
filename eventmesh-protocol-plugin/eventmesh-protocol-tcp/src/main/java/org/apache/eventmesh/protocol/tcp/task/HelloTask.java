/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.protocol.tcp.task;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.eventmesh.common.config.CommonConfiguration;
import org.apache.eventmesh.common.protocol.tcp.Header;
import org.apache.eventmesh.common.protocol.tcp.OPStatus;
import org.apache.eventmesh.common.protocol.tcp.Package;
import org.apache.eventmesh.common.protocol.tcp.UserAgent;
import org.apache.eventmesh.protocol.api.model.ServiceState;
import org.apache.eventmesh.protocol.tcp.EventMeshProtocolTCPServer;
import org.apache.eventmesh.protocol.tcp.acl.Acl;
import org.apache.eventmesh.protocol.tcp.client.session.Session;
import org.apache.eventmesh.protocol.tcp.config.TcpProtocolConstants;
import org.apache.eventmesh.protocol.tcp.utils.ChannelUtils;
import org.apache.eventmesh.protocol.tcp.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.eventmesh.common.protocol.tcp.Command.HELLO_REQUEST;
import static org.apache.eventmesh.common.protocol.tcp.Command.HELLO_RESPONSE;

public class HelloTask extends AbstractTask {

    private final Logger messageLogger = LoggerFactory.getLogger("message");

    public HelloTask(Package pkg, ChannelHandlerContext ctx, long startTime, EventMeshProtocolTCPServer eventMeshTCPServer) {
        super(pkg, ctx, startTime, eventMeshTCPServer);
    }

    @Override
    public void run() {
        long taskExecuteTime = System.currentTimeMillis();
        Package res = new Package();
        Session session = null;
        UserAgent user = (UserAgent) pkg.getBody();
        try {
            //do acl check in connect
            if (CommonConfiguration.eventMeshServerSecurityEnable) {
                String remoteAddr = ChannelUtils.parseChannelRemoteAddr(ctx.channel());
                Acl.doAclCheckInTcpConnect(remoteAddr, user, HELLO_REQUEST.value());
            }

            if (eventMeshTCPServer.getServiceState() != ServiceState.RUNNING) {
                logger.error("server state is not running:{}", eventMeshTCPServer.getServiceState());
                throw new Exception("server state is not running, maybe deploying...");
            }

            validateUserAgent(user);
            session = eventMeshTCPServer.getClientSessionGroupMapping().createSession(user, ctx);
            res.setHeader(new Header(HELLO_RESPONSE, OPStatus.SUCCESS.getCode(), OPStatus.SUCCESS.getDesc(), pkg.getHeader().getSeq()));
            Utils.writeAndFlush(res, startTime, taskExecuteTime, session.getContext(), session);
        } catch (Throwable e) {
            messageLogger.error("HelloTask failed|address={},errMsg={}", ctx.channel().remoteAddress(), e);
            res.setHeader(new Header(HELLO_RESPONSE, OPStatus.FAIL.getCode(), e.getStackTrace().toString(), pkg
                    .getHeader().getSeq()));
            ctx.writeAndFlush(res).addListener(
                    new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (!future.isSuccess()) {
                                Utils.logFailedMessageFlow(future, res, user, startTime, taskExecuteTime);
                            } else {
                                Utils.logSucceedMessageFlow(res, user, startTime, taskExecuteTime);
                            }
                            logger.warn("HelloTask failed,close session,addr:{}", ctx.channel().remoteAddress());
                            eventMeshTCPServer.getClientSessionGroupMapping().closeSession(ctx);
                        }
                    }
            );
        }
    }

    private void validateUserAgent(UserAgent user) throws Exception {
        if (user == null) {
            throw new Exception("client info cannot be null");
        }

        if (user.getVersion() == null) {
            throw new Exception("client version cannot be null");
        }

        if (!(StringUtils.equals(TcpProtocolConstants.PURPOSE_PUB, user.getPurpose())
                || StringUtils.equals(TcpProtocolConstants.PURPOSE_SUB, user.getPurpose()))) {
            throw new Exception("client purpose config is error");
        }

        if (StringUtils.equals(TcpProtocolConstants.PURPOSE_PUB, user.getPurpose())
                && StringUtils.isBlank(user.getProducerGroup())) {
            throw new Exception("client producerGroup cannot be null");
        }

        if (StringUtils.equals(TcpProtocolConstants.PURPOSE_SUB, user.getPurpose())
                && StringUtils.isBlank(user.getConsumerGroup())) {
            throw new Exception("client consumerGroup cannot be null");
        }
    }
}