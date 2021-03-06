package com.feed_the_beast.ftblib.events.client;

import com.feed_the_beast.ftblib.events.FTBLibEvent;
import com.feed_the_beast.ftblib.lib.gui.GuiHelper;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

/**
 * @author LatvianModder
 */
@Cancelable
public class GuideEvent extends FTBLibEvent
{
	public static boolean check(String path)
	{
		return new Check(path).post();
	}

	public static boolean open(String path)
	{
		return new Open(path).post();
	}

	public static boolean openOrWeb(String path)
	{
		if (!open(path))
		{
			GuiHelper.BLANK_GUI.handleClick("https://guides.latmod.com" + path);
		}

		return true;
	}

	private final String path;

	private GuideEvent(String p)
	{
		path = p;
	}

	public static class Check extends GuideEvent
	{
		private Check(String path)
		{
			super(path);
		}
	}

	public static class Open extends GuideEvent
	{
		private Open(String path)
		{
			super(path);
		}
	}

	public String getPath()
	{
		return path;
	}
}