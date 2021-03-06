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

package io.bitsquare.util.tasks;

import io.bitsquare.util.handlers.FaultHandler;
import io.bitsquare.util.handlers.ResultHandler;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jetbrains.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskRunner<T extends SharedModel> {
    private static final Logger log = LoggerFactory.getLogger(TaskRunner.class);

    private final Queue<Class> tasks = new LinkedBlockingQueue<>();
    protected final T sharedModel;
    private final ResultHandler resultHandler;
    private final FaultHandler faultHandler;

    private boolean failed = false;
    private Class<? extends Task> currentTask;
    private Class<? extends Task> previousTask;

    public TaskRunner(T sharedModel, ResultHandler resultHandler, FaultHandler faultHandler) {
        this.sharedModel = sharedModel;
        this.resultHandler = resultHandler;
        this.faultHandler = faultHandler;
    }

    public void run() {
        next();
    }

    public Class<? extends Task> getCurrentTask() {
        return currentTask;
    }

    protected void next() {
        if (!failed) {
            if (tasks.size() > 0) {
                try {
                    setCurrentTask(tasks.poll());
                    log.trace("Run task: " + currentTask.getSimpleName());
                    currentTask.getDeclaredConstructor(TaskRunner.class, sharedModel.getClass()).newInstance(this, sharedModel).run();
                    setPreviousTask(currentTask);
                } catch (Throwable t) {
                    t.printStackTrace();
                    faultHandler.handleFault(t.getMessage(), t);
                }
            }
            else {
                resultHandler.handleResult();
            }
        }
    }

    protected void setPreviousTask(Class<? extends Task> task) {
        previousTask = task;
    }

    protected void setCurrentTask(Class<? extends Task> task) {
        currentTask = task;
    }

    public void addTask(Class<? extends Task> task) {
        tasks.add(task);
    }

    public void addTasks(Class<? extends Task>... items) {
        tasks.addAll(Arrays.asList(items));
    }

    public void complete() {
        next();
    }

    public void handleFault(String message) {
        handleFault(message, new Exception(message));
    }

    public void handleFault(String message, @NotNull Throwable throwable) {
        log.debug(throwable.getMessage());
        failed = true;
        faultHandler.handleFault(message, throwable);
    }
}
