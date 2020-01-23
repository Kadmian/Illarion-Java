/*
 * This file is part of the Illarion project.
 *
 * Copyright © 2015 - Illarion e.V.
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
package illarion.client.states;

import de.lessvoid.nifty.Nifty;
import illarion.client.Game;
import illarion.client.input.InputReceiver;
import illarion.client.world.MapDimensions;
import illarion.client.world.World;
import org.illarion.engine.GameContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This state is active while the player is playing the game.
 *
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
public class PlayingState implements GameState {
    /**
     * The input receiver of the game.
     */
    @Nonnull
    private final InputReceiver receiver;

    /**
     * The logger that is used for the logging output of this class.
     */
    @Nonnull
    private static final Logger log = LoggerFactory.getLogger(PlayingState.class);

    @Nullable
    private Game game;

    public PlayingState(@Nonnull InputReceiver inputReceiver) {
        receiver = inputReceiver;
    }

    @Override
    public void create(@Nonnull Game game, @Nonnull GameContainer container, @Nonnull Nifty nifty) {
        this.game = game;

        log.trace("Creating playing state.");
        World.initGui(container.getEngine());
        nifty.registerScreenController(World.getGameGui().getScreenController());

        Util.loadXML(nifty, "illarion/client/gui/xml/gamescreen.xml");
    }

    @Override
    public void dispose() {
    }

    @Override
    public void resize(@Nonnull GameContainer container, int width, int height) {
        MapDimensions.getInstance().reportScreenSize(width, height, false);
    }

    @Override
    public void update(@Nonnull GameContainer container, int delta) {
        if (World.getGameGui().isReady()) {
            World.getUpdateTaskManager().onUpdateGame(container, delta);
        }
        World.getGameGui().onUpdateGame(container, delta);
        World.getWeather().update(delta);
        World.getMapDisplay().update(container, delta);
        World.getAnimationManager().animate(delta);
        World.getMusicBox().update();
    }

    @Override
    public void render(@Nonnull GameContainer container) {
        World.getMap().getMiniMap().render(container);
        World.getMapDisplay().render(container);
    }

    @Override
    public boolean isClosingGame() {
        World.getGameGui().getCloseGameGui().showClosingDialog();
        return false;
    }

    @Override
    public void enterState(@Nonnull GameContainer container, @Nonnull Nifty nifty) {
        nifty.gotoScreen("gamescreen");
        receiver.setEnabled(true);
    }

    @Override
    public void leaveState(@Nonnull GameContainer container) {
        receiver.setEnabled(false);
    }
}
