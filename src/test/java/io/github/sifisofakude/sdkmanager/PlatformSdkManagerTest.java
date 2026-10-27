package io.github.sifisofakude.sdkmanager;

import java.util.Map;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlatformSdkManagerTest	{
	private AndroidSdkManager sdkmanager;
	
	public PlatformSdkManagerTest()	{
		sdkmanager = new AndroidSdkManager();
	}
	
	@Test
	public void downloadPlatform() throws Exception	{
		// PlatformsSdkManager sdkmanager = new PlatformsSdkManager();
		assertTrue(sdkmanager.downloadPlatform("platforms;android-36","../libs"));
	}

	@Test
	public void downloadRepoConfigFile()	{
		try	{
			InputStream in = sdkmanager.downloadRepoConfig();
			assertTrue(in != null);

			Map<String, String> parsed = sdkmanager.parsePlatformsRepoFile("platforms;android-36",in);
			System.out.println(parsed);
		}catch(Exception e)	{}
	}
}
