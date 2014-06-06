/*
 * This file is part of the Illarion project.
 *
 * Copyright © 2014 - Illarion e.V.
 *
 * Illarion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Illarion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package illarion.client.world.movement;

import illarion.client.graphics.AnimatedMove;
import illarion.client.graphics.MoveAnimation;
import illarion.client.net.client.MoveCmd;
import illarion.client.net.client.TurnCmd;
import illarion.client.world.CharMovementMode;
import illarion.client.world.Player;
import illarion.client.world.World;
import illarion.common.types.CharacterId;
import illarion.common.types.Location;
import illarion.common.util.Timer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.illarion.engine.input.Input;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This is the main controlling class for the movement. It maintains the references to the different handlers and
 * makes sure that the movement commands of the handlers are put in action.
 *
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
@Slf4j
public class Movement {
    /**
     * The instance of the player that is moved around by this class.
     */
    @Nonnull
    @Getter(AccessLevel.PACKAGE)
    private final Player player;

    /**
     * The currently active movement handler.
     */
    @Nullable
    private MovementHandler activeHandler;

    @Nonnull
    @Getter
    @Setter
    private CharMovementMode defaultMovementMode;

    private boolean stepInProgress;

    @Nonnull
    private final MoveAnimator animator;

    @Nonnull
    @Getter
    private final MouseMovementHandler followMouseHandler;

    @Nonnull
    @Getter
    private final KeyboardMovementHandler keyboardHandler;

    @Nonnull
    @Getter
    private final TargetMovementHandler targetMovementHandler;

    @Nonnull
    private final MoveAnimation moveAnimation;

    @Nonnull
    private final Timer timeoutTimer;

    public Movement(@Nonnull Player player, @Nonnull Input input, @Nonnull AnimatedMove movementReceiver) {
        this.player = player;
        moveAnimation = new MoveAnimation(movementReceiver);
        defaultMovementMode = CharMovementMode.Walk;
        stepInProgress = false;
        animator = new MoveAnimator(this, moveAnimation);
        moveAnimation.addTarget(animator, false);

        followMouseHandler = new FollowMouseMovementHandler(this, input);
        keyboardHandler = new SimpleKeyboardMovementHandler(this, input);
        targetMovementHandler = new WalkToMovementHandler(this);

        timeoutTimer = new Timer(700, new Runnable() {
            @Override
            public void run() {
                reportReadyForNextStep();
            }
        });
    }

    public boolean isMoving() {
        return moveAnimation.isRunning();
    }

    void activate(@Nonnull MovementHandler handler) {
        if (!isActive(handler)) {
            @val MovementHandler oldHandler = activeHandler;
            if (oldHandler != null) {
                oldHandler.disengage();
            }
            activeHandler = handler;
            log.info("New movement handler is assuming control: {}", activeHandler);
        }
        update();
    }

    void disengage(@Nonnull MovementHandler handler) {
        if (isActive(handler)) {
            activeHandler = null;
        } else {
            log.warn("Tried to disengage a movement handler ({}) that was not active!", handler);
        }
    }

    boolean isActive(@Nonnull MovementHandler handler) {
        return (activeHandler != null) && handler.equals(activeHandler);
    }

    public void executeServerRespTurn(int direction) {
        animator.scheduleTurn(direction);
    }

    public void executeServerRespMove(@Nonnull CharMovementMode mode, @Nonnull Location target, int speed) {
        timeoutTimer.stop();
        animator.scheduleMove(mode, target, speed);
    }

    /**
     * Send the movement command to the server.
     *
     * @param direction the direction of the requested move
     * @param mode the mode of the requested move
     */
    private void sendMoveToServer(int direction, @Nonnull CharMovementMode mode) {
        if (!Location.isValidDirection(direction)) {
            throw new IllegalArgumentException("Direction is out of the valid range.");
        }
        CharacterId playerId = player.getPlayerId();
        if (playerId == null) {
            log.error("Send move to server while ID is not known.");
            return;
        }
        World.getNet().sendCommand(new MoveCmd(playerId, mode, direction));
        timeoutTimer.stop();
        timeoutTimer.start();
    }

    private void sendTurnToServer(int direction) {
        if (!Location.isValidDirection(direction)) {
            throw new IllegalArgumentException("Direction is out of the valid range.");
        }
        if (player.getCharacter().getDirection() != direction) {
            World.getNet().sendCommand(new TurnCmd(direction));
        }
    }

    private void requestNextMove(@Nonnull StepData nextStep) {
        switch (nextStep.getMovementMode()) {
            case Walk:
            case Run:
                sendTurnToServer(nextStep.getDirection());
                sendMoveToServer(nextStep.getDirection(), nextStep.getMovementMode());
                break;
            default:
                throw new IllegalStateException("The returned next step did not contain a valid movement mode.");
        }
    }

    /**
     * Notify the handler that everything is ready to request the next step from the server.
     */
    void reportReadyForNextStep() {
        stepInProgress = false;
        update();
    }

    /**
     * This function triggers the lifecycle run of the movement handler. Its executed automatically as the movement
     * handler sees fit. How ever for special events that might require a action of the movement handler this function
     * can be triggered.
     * <p/>
     * Calling it too often causes no harm. The handler checks internally if any actual operations need to be executed
     * or not.
     */
    public void update() {
        if (stepInProgress) {
            return;
        }
        MovementHandler handler = activeHandler;
        if (handler != null) {
            StepData nextStep = handler.getNextStep();
            log.info("Requesting new step data from handler: {}", nextStep);
            if (nextStep.getMovementMode() != CharMovementMode.None) {
                stepInProgress = true;
                requestNextMove(nextStep);
            }
        }
    }
}