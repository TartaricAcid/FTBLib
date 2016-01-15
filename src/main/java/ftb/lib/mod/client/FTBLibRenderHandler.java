package ftb.lib.mod.client;

import ftb.lib.api.gui.callback.ClientTickCallback;
import ftb.lib.client.FTBLibClient;
import ftb.lib.notification.ClientNotifications;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.*;

import java.util.*;

@SideOnly(Side.CLIENT)
public class FTBLibRenderHandler
{
	public static final FTBLibRenderHandler instance = new FTBLibRenderHandler();
	public static final List<ClientTickCallback> callbacks = new ArrayList<>();
	
	@SubscribeEvent(priority = EventPriority.LOW)
	public void renderTick(TickEvent.RenderTickEvent e)
	{
		GlStateManager.pushMatrix();
		GlStateManager.pushAttrib();
		
		if(e.phase == TickEvent.Phase.START)
		{
			ScaledResolution sr = new ScaledResolution(FTBLibClient.mc);
			FTBLibClient.displayW = sr.getScaledWidth();
			FTBLibClient.displayH = sr.getScaledHeight();
		}
		
		if(e.phase == TickEvent.Phase.END && FTBLibClient.isPlaying()) ClientNotifications.renderTemp();
		
		GlStateManager.popAttrib();
		GlStateManager.popMatrix();
	}
	
	@SubscribeEvent
	public void clientTick(TickEvent.ClientTickEvent e)
	{
		if(e.phase == TickEvent.Phase.END && !callbacks.isEmpty())
		{
			for(int i = 0; i < callbacks.size(); i++)
				callbacks.get(i).onCallback();
			callbacks.clear();
		}
	}
}