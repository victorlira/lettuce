package com.lambdaworks.redis.cluster;

import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;
import com.lambdaworks.redis.protocol.CommandWrapper;
import com.lambdaworks.redis.protocol.ProtocolKeyword;
import com.lambdaworks.redis.protocol.RedisCommand;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
class ClusterCommand<K, V, T> extends CommandWrapper<K, V, T> implements RedisCommand<K, V, T> {

    private RedisChannelWriter<K, V> retry;
    private int executions;
    private int executionLimit;
    private boolean completed;

    ClusterCommand(RedisCommand<K, V, T> command, RedisChannelWriter<K, V> retry, int executionLimit) {
        super(command);
        this.retry = retry;
        this.executionLimit = executionLimit;
    }

    @Override
    public void complete() {
        executions++;

        if (executions < executionLimit && isMoved()) {
            retry.write(this);
            return;
        }
        super.complete();
        completed = true;
    }

    public boolean isMoved() {
        if (command.getOutput() != null && command.getOutput().getError() != null
                && command.getOutput().getError().startsWith(CommandKeyword.MOVED.name())) {
            return true;
        }
        return false;
    }

    @Override
    public CommandArgs<K, V> getArgs() {
        return command.getArgs();
    }

    public int getExecutions() {
        return executions;
    }

    public int getExecutionLimit() {
        return executionLimit;
    }

    @Override
    public void encode(ByteBuf buf) {
        command.encode(buf);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        boolean result = command.completeExceptionally(ex);
        completed = true;
        return result;
    }

    @Override
    public ProtocolKeyword getType() {
        return command.getType();
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getError() {
        if (command.getOutput() != null) {
            return command.getOutput().getError();
        }
        return null;
    }
}
