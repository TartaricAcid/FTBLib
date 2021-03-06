package com.feed_the_beast.ftblib.lib.gui;

import com.feed_the_beast.ftblib.lib.icon.Color4I;
import com.feed_the_beast.ftblib.lib.icon.Icon;
import com.feed_the_beast.ftblib.lib.io.Bits;
import com.feed_the_beast.ftblib.lib.util.misc.MouseButton;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Widget implements IGuiWrapper
{
	protected static final int DARK = 1;
	protected static final int SHADOW = 2;
	protected static final int CENTERED = 4;
	protected static final int UNICODE = 8;
	protected static final int MOUSE_OVER = 16;

	public Panel parent;
	public int posX, posY, width, height;
	private boolean isMouseOver;

	public Widget(Panel p)
	{
		parent = p;
	}

	@Override
	public GuiBase getGui()
	{
		return parent.getGui();
	}

	public Theme getTheme()
	{
		return parent.getTheme();
	}

	public void setX(int v)
	{
		posX = v;
	}

	public void setY(int v)
	{
		posY = v;
	}

	public void setWidth(int v)
	{
		width = Math.max(v, 0);
	}

	public void setHeight(int v)
	{
		height = Math.max(v, 0);
	}

	public final void setPos(int x, int y)
	{
		setX(x);
		setY(y);
	}

	public final void setSize(int w, int h)
	{
		setWidth(w);
		setHeight(h);
	}

	public final Widget setPosAndSize(int x, int y, int w, int h)
	{
		setX(x);
		setY(y);
		setWidth(w);
		setHeight(h);
		return this;
	}

	public int getAX()
	{
		return parent.getAX() + posX;
	}

	public int getAY()
	{
		return parent.getAY() + posY;
	}

	public boolean collidesWith(int x, int y, int w, int h)
	{
		int ay = getAY();
		if (ay >= y + h || ay + height <= y)
		{
			return false;
		}

		int ax = getAX();
		return ax < x + w && ax + width > x;
	}

	public boolean isEnabled()
	{
		return true;
	}

	public boolean shouldDraw()
	{
		return true;
	}

	public String getTitle()
	{
		return "";
	}

	public Icon getIcon()
	{
		return Icon.EMPTY;
	}

	public WidgetType getWidgetType()
	{
		return WidgetType.mouseOver(isMouseOver());
	}

	public void addMouseOverText(List<String> list)
	{
		String title = getTitle();

		if (!title.isEmpty())
		{
			list.add(title);
		}
	}

	public final boolean isMouseOver()
	{
		return isMouseOver;
	}

	public boolean checkMouseOver(int mouseX, int mouseY)
	{
		if (parent == null)
		{
			return true;
		}
		else if (!parent.isMouseOver())
		{
			return false;
		}

		int ax = getAX();
		int ay = getAY();
		return mouseX >= ax && mouseY >= ay && mouseX < ax + width && mouseY < ay + height;
	}

	public void updateMouseOver(int mouseX, int mouseY)
	{
		isMouseOver = checkMouseOver(mouseX, mouseY);
	}

	public boolean shouldAddMouseOverText()
	{
		return isEnabled() && isMouseOver();
	}

	public void draw()
	{
		getIcon().draw(getAX(), getAY(), width, height);
	}

	public boolean mousePressed(MouseButton button)
	{
		return false;
	}

	public void mouseReleased(MouseButton button)
	{
	}

	public boolean mouseScrolled(int scroll)
	{
		return false;
	}

	public boolean keyPressed(int key, char keyChar)
	{
		return false;
	}

	public void keyReleased(int key)
	{
	}

	public ScaledResolution getScreen()
	{
		return parent.getScreen();
	}

	public int getMouseX()
	{
		return parent.getMouseX();
	}

	public int getMouseY()
	{
		return parent.getMouseY();
	}

	public final boolean isMouseButtonDown(MouseButton button)
	{
		return Mouse.isButtonDown(button.id);
	}

	public final boolean isKeyDown(int key)
	{
		return Keyboard.isKeyDown(key);
	}

	public FontRenderer getFont()
	{
		return parent.getFont();
	}

	public final int getStringWidth(String text)
	{
		return getFont().getStringWidth(text);
	}

	public final int getFontHeight()
	{
		return getFont().FONT_HEIGHT;
	}

	public final String trimStringToWidth(String text, int width)
	{
		return text.isEmpty() ? "" : getFont().trimStringToWidth(text, width, false);
	}

	public final String trimStringToWidthReverse(String text, int width)
	{
		return text.isEmpty() ? "" : getFont().trimStringToWidth(text, width, true);
	}

	public final List<String> listFormattedStringToWidth(String text, int width)
	{
		if (width <= 0 || text.isEmpty())
		{
			return Collections.emptyList();
		}

		return getFont().listFormattedStringToWidth(text, width);
	}

	public final int drawString(String text, int x, int y, Color4I color, int flags)
	{
		if (text.isEmpty() || color.isEmpty())
		{
			return 0;
		}

		if (Bits.getFlag(flags, CENTERED))
		{
			x -= getStringWidth(text) / 2;
		}

		int i = getFont().drawString(text, x, y, color.rgba(), Bits.getFlag(flags, SHADOW));
		GlStateManager.color(1F, 1F, 1F, 1F);
		return i;
	}

	public final int drawString(String text, int x, int y, int flags)
	{
		return drawString(text, x, y, getTheme().getContentColor(WidgetType.mouseOver(Bits.getFlag(flags, MOUSE_OVER))), flags);
	}

	public final int drawString(String text, int x, int y)
	{
		return drawString(text, x, y, getTheme().getContentColor(WidgetType.NORMAL), 0);
	}

	public void pushFontUnicode(boolean flag)
	{
		parent.pushFontUnicode(flag);
	}

	public void popFontUnicode()
	{
		parent.popFontUnicode();
	}

	public boolean handleClick(String scheme, String path)
	{
		return parent.handleClick(scheme, path);
	}

	public final boolean handleClick(String click)
	{
		int index = click.indexOf(':');

		if (index == -1)
		{
			return handleClick("", click);
		}

		return handleClick(click.substring(0, index), click.substring(index + 1));
	}

	public List<GuiBase.PositionedTextData> createDataFrom(ITextComponent component, int width)
	{
		if (width <= 0 || component.getUnformattedText().isEmpty())
		{
			return Collections.emptyList();
		}

		List<GuiBase.PositionedTextData> list = new ArrayList<>();

		int line = 0;
		int currentWidth = 0;

		for (ITextComponent t : component.createCopy())
		{
			String text = t.getUnformattedComponentText();
			int textWidth = getStringWidth(text);

			while (textWidth > 0)
			{
				int w = textWidth;
				if (w > width - currentWidth)
				{
					w = width - currentWidth;
				}

				list.add(new GuiBase.PositionedTextData(currentWidth, line * 10, w, 10, t.getStyle()));

				currentWidth += w;
				textWidth -= w;

				if (currentWidth >= width)
				{
					currentWidth = 0;
					line++;
				}
			}
		}

		return list;
	}

	public void onClosed()
	{
	}

	public static String getClipboardString()
	{
		return GuiScreen.getClipboardString();
	}

	public static void setClipboardString(String string)
	{
		GuiScreen.setClipboardString(string);
	}

	public static boolean isShiftKeyDown()
	{
		return GuiScreen.isShiftKeyDown();
	}

	public static boolean isCtrlKeyDown()
	{
		return GuiScreen.isCtrlKeyDown();
	}

	public static boolean isAltKeyDown()
	{
		return GuiScreen.isAltKeyDown();
	}

	public static boolean isKeyComboCtrlX(int key)
	{
		return GuiScreen.isKeyComboCtrlX(key);
	}

	public static boolean isKeyComboCtrlV(int key)
	{
		return GuiScreen.isKeyComboCtrlV(key);
	}

	public static boolean isKeyComboCtrlC(int key)
	{
		return GuiScreen.isKeyComboCtrlC(key);
	}

	public static boolean isKeyComboCtrlA(int key)
	{
		return GuiScreen.isKeyComboCtrlA(key);
	}
}