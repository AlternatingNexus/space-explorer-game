package com.spaceexplorer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

/**
 * The window content: owns nothing but the keyboard plumbing, and asks the
 * {@link Game} to render itself. Swing gives us double buffering, so
 * {@code paintComponent} only has to produce one finished frame.
 */
public final class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Game game;

    public GamePanel(Game game, InputHandler input) {
        this.game = game;
        setPreferredSize(new Dimension(Game.VIEW_WIDTH, Game.VIEW_HEIGHT));
        setBackground(Palette.VOID);
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                input.onKeyPressed(event);
            }

            @Override
            public void keyReleased(KeyEvent event) {
                input.onKeyReleased(event);
            }
        });

        // Only used by the RETRY button on the failure screen.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                game.onMouseClick(event.getX(), event.getY(), getWidth(), getHeight());
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        game.render(g2d, getWidth(), getHeight());
        g2d.dispose();
    }
}
