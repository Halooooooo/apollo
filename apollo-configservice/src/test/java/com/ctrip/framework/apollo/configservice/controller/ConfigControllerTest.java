package com.ctrip.framework.apollo.configservice.controller;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.ctrip.framework.apollo.biz.entity.AppNamespace;
import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.service.AppNamespaceService;
import com.ctrip.framework.apollo.biz.service.ConfigService;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigControllerTest {
  private ConfigController configController;
  @Mock
  private ConfigService configService;
  @Mock
  private AppNamespaceService appNamespaceService;
  private String someAppId;
  private String someClusterName;
  private String defaultClusterName;
  private String defaultNamespaceName;
  private String somePublicNamespaceName;
  private String someDataCenter;
  private String someClientIp;
  @Mock
  private Release someRelease;
  @Mock
  private Release somePublicRelease;

  @Before
  public void setUp() throws Exception {
    configController = new ConfigController();
    ReflectionTestUtils.setField(configController, "configService", configService);
    ReflectionTestUtils.setField(configController, "appNamespaceService", appNamespaceService);

    someAppId = "1";
    someClusterName = "someClusterName";
    defaultClusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
    defaultNamespaceName = ConfigConsts.NAMESPACE_DEFAULT;
    somePublicNamespaceName = "somePublicNamespace";
    someDataCenter = "someDC";
    someClientIp = "someClientIp";
    String someValidConfiguration = "{\"apollo.bar\": \"foo\"}";
    String somePublicConfiguration = "{\"apollo.public.bar\": \"foo\"}";

    when(someRelease.getClusterName()).thenReturn(someClusterName);
    when(someRelease.getConfigurations()).thenReturn(someValidConfiguration);
    when(somePublicRelease.getConfigurations()).thenReturn(somePublicConfiguration);
  }

  @Test
  public void testQueryConfig() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideNewReleaseKey = "2";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);

    when(configService.findRelease(someAppId, someClusterName, defaultNamespaceName))
        .thenReturn(someRelease);
    when(someRelease.getReleaseKey()).thenReturn(someServerSideNewReleaseKey);

    ApolloConfig result = configController.queryConfig(someAppId, someClusterName,
        defaultNamespaceName, someDataCenter, someClientSideReleaseKey,
        someClientIp, someResponse);

    verify(configService, times(1)).findRelease(someAppId, someClusterName, defaultNamespaceName);
    assertEquals(someAppId, result.getAppId());
    assertEquals(someClusterName, result.getCluster());
    assertEquals(defaultNamespaceName, result.getNamespaceName());
    assertEquals(someServerSideNewReleaseKey, result.getReleaseKey());
  }


  @Test
  public void testQueryConfigWithReleaseNotFound() throws Exception {
    String someClientSideReleaseKey = "1";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);

    when(configService.findRelease(someAppId, someClusterName, defaultNamespaceName))
        .thenReturn(null);

    ApolloConfig result = configController.queryConfig(someAppId, someClusterName,
        defaultNamespaceName, someDataCenter, someClientSideReleaseKey,
        someClientIp, someResponse);

    assertNull(result);
    verify(someResponse, times(1)).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
  }

  @Test
  public void testQueryConfigWithApolloConfigNotModified() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideReleaseKey = someClientSideReleaseKey;
    HttpServletResponse someResponse = mock(HttpServletResponse.class);

    when(configService.findRelease(someAppId, someClusterName, defaultNamespaceName))
        .thenReturn(someRelease);
    when(someRelease.getReleaseKey()).thenReturn(someServerSideReleaseKey);

    ApolloConfig
        result =
        configController.queryConfig(someAppId, someClusterName, defaultNamespaceName,
            someDataCenter, String.valueOf(someClientSideReleaseKey), someClientIp, someResponse);

    assertNull(result);
    verify(someResponse, times(1)).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
  }

  @Test
  public void testQueryConfigWithDefaultClusterWithDataCenterRelease() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideNewReleaseKey = "2";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);

    when(configService.findRelease(someAppId, someDataCenter, defaultNamespaceName))
        .thenReturn(someRelease);
    when(someRelease.getReleaseKey()).thenReturn(someServerSideNewReleaseKey);
    when(someRelease.getClusterName()).thenReturn(someDataCenter);

    ApolloConfig result = configController.queryConfig(someAppId, defaultClusterName,
        defaultNamespaceName, someDataCenter, someClientSideReleaseKey,
        someClientIp, someResponse);

    verify(configService, times(1)).findRelease(someAppId, someDataCenter, defaultNamespaceName);
    assertEquals(someAppId, result.getAppId());
    assertEquals(someDataCenter, result.getCluster());
    assertEquals(defaultNamespaceName, result.getNamespaceName());
    assertEquals(someServerSideNewReleaseKey, result.getReleaseKey());
  }

  @Test
  public void testQueryConfigWithDefaultClusterWithNoDataCenterRelease() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideNewReleaseKey = "2";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);

    when(configService.findRelease(someAppId, someDataCenter, defaultNamespaceName))
        .thenReturn(null);
    when(configService.findRelease(someAppId, defaultClusterName, defaultNamespaceName))
        .thenReturn(someRelease);
    when(someRelease.getReleaseKey()).thenReturn(someServerSideNewReleaseKey);
    when(someRelease.getClusterName()).thenReturn(defaultClusterName);

    ApolloConfig result = configController.queryConfig(someAppId, defaultClusterName,
        defaultNamespaceName, someDataCenter, someClientSideReleaseKey,
        someClientIp, someResponse);

    verify(configService, times(1)).findRelease(someAppId, someDataCenter, defaultNamespaceName);
    verify(configService, times(1))
        .findRelease(someAppId, defaultClusterName, defaultNamespaceName);
    assertEquals(someAppId, result.getAppId());
    assertEquals(defaultClusterName, result.getCluster());
    assertEquals(defaultNamespaceName, result.getNamespaceName());
    assertEquals(someServerSideNewReleaseKey, result.getReleaseKey());


  }

  @Test
  public void testQueryConfigWithAppOwnNamespace() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideReleaseKey = "2";
    String someAppOwnNamespaceName = "someAppOwn";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    AppNamespace someAppOwnNamespace =
        assmbleAppNamespace(someAppId, someAppOwnNamespaceName);

    when(configService.findRelease(someAppId, someClusterName, someAppOwnNamespaceName))
        .thenReturn(someRelease);
    when(appNamespaceService.findByNamespaceName(someAppOwnNamespaceName))
        .thenReturn(someAppOwnNamespace);
    when(someRelease.getReleaseKey()).thenReturn(someServerSideReleaseKey);

    ApolloConfig result =
        configController
            .queryConfig(someAppId, someClusterName, someAppOwnNamespaceName, someDataCenter,
                someClientSideReleaseKey, someClientIp, someResponse);

    assertEquals(someServerSideReleaseKey, result.getReleaseKey());
    assertEquals(someAppId, result.getAppId());
    assertEquals(someClusterName, result.getCluster());
    assertEquals(someAppOwnNamespaceName, result.getNamespaceName());
    assertEquals("foo", result.getConfigurations().get("apollo.bar"));
  }

  @Test
  public void testQueryConfigWithPubicNamespaceAndNoAppOverride() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideReleaseKey = "2";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    String somePublicAppId = "somePublicAppId";
    AppNamespace somePublicAppNamespace =
        assmbleAppNamespace(somePublicAppId, somePublicNamespaceName);

    when(configService.findRelease(someAppId, someClusterName, somePublicNamespaceName))
        .thenReturn(null);
    when(appNamespaceService.findByNamespaceName(somePublicNamespaceName))
        .thenReturn(somePublicAppNamespace);
    when(configService.findRelease(somePublicAppId, someDataCenter, somePublicNamespaceName))
        .thenReturn(somePublicRelease);
    when(somePublicRelease.getReleaseKey()).thenReturn(someServerSideReleaseKey);

    ApolloConfig result =
        configController
            .queryConfig(someAppId, someClusterName, somePublicNamespaceName, someDataCenter,
                someClientSideReleaseKey, someClientIp, someResponse);

    assertEquals(someServerSideReleaseKey, result.getReleaseKey());
    assertEquals(someAppId, result.getAppId());
    assertEquals(someClusterName, result.getCluster());
    assertEquals(somePublicNamespaceName, result.getNamespaceName());
    assertEquals("foo", result.getConfigurations().get("apollo.public.bar"));
  }

  @Test
  public void testQueryConfigWithPublicNamespaceAndNoAppOverrideAndNoDataCenter() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideReleaseKey = "2";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    String somePublicAppId = "somePublicAppId";
    AppNamespace somePublicAppNamespace =
        assmbleAppNamespace(somePublicAppId, somePublicNamespaceName);

    when(configService.findRelease(someAppId, someClusterName, somePublicNamespaceName))
        .thenReturn(null);
    when(appNamespaceService.findByNamespaceName(somePublicNamespaceName))
        .thenReturn(somePublicAppNamespace);
    when(configService.findRelease(somePublicAppId, someDataCenter, somePublicNamespaceName))
        .thenReturn(null);
    when(configService
        .findRelease(somePublicAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, somePublicNamespaceName))
        .thenReturn(somePublicRelease);
    when(somePublicRelease.getReleaseKey()).thenReturn(someServerSideReleaseKey);

    ApolloConfig result =
        configController
            .queryConfig(someAppId, someClusterName, somePublicNamespaceName, someDataCenter,
                someClientSideReleaseKey, someClientIp, someResponse);

    assertEquals(someServerSideReleaseKey, result.getReleaseKey());
    assertEquals(someAppId, result.getAppId());
    assertEquals(someClusterName, result.getCluster());
    assertEquals(somePublicNamespaceName, result.getNamespaceName());
    assertEquals("foo", result.getConfigurations().get("apollo.public.bar"));
  }

  @Test
  public void testQueryConfigWithPublicNamespaceAndAppOverride() throws Exception {
    String someAppSideReleaseKey = "1";
    String somePublicAppSideReleaseKey = "2";

    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    String somePublicAppId = "somePublicAppId";
    AppNamespace somePublicAppNamespace =
        assmbleAppNamespace(somePublicAppId, somePublicNamespaceName);

    when(someRelease.getConfigurations()).thenReturn("{\"apollo.public.foo\": \"foo-override\"}");
    when(somePublicRelease.getConfigurations())
        .thenReturn("{\"apollo.public.foo\": \"foo\", \"apollo.public.bar\": \"bar\"}");

    when(configService.findRelease(someAppId, someClusterName, somePublicNamespaceName))
        .thenReturn(someRelease);
    when(someRelease.getReleaseKey()).thenReturn(someAppSideReleaseKey);
    when(appNamespaceService.findByNamespaceName(somePublicNamespaceName))
        .thenReturn(somePublicAppNamespace);
    when(configService.findRelease(somePublicAppId, someDataCenter, somePublicNamespaceName))
        .thenReturn(somePublicRelease);
    when(somePublicRelease.getReleaseKey()).thenReturn(somePublicAppSideReleaseKey);

    ApolloConfig result =
        configController
            .queryConfig(someAppId, someClusterName, somePublicNamespaceName, someDataCenter,
                someAppSideReleaseKey, someClientIp, someResponse);

    assertEquals(Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
            .join(someAppSideReleaseKey, somePublicAppSideReleaseKey),
        result.getReleaseKey());
    assertEquals(someAppId, result.getAppId());
    assertEquals(someClusterName, result.getCluster());
    assertEquals(somePublicNamespaceName, result.getNamespaceName());
    assertEquals("foo-override", result.getConfigurations().get("apollo.public.foo"));
    assertEquals("bar", result.getConfigurations().get("apollo.public.bar"));
  }

  @Test
  public void testMergeConfigurations() throws Exception {
    Gson gson = new Gson();
    String key1 = "key1";
    String value1 = "value1";
    String anotherValue1 = "anotherValue1";

    String key2 = "key2";
    String value2 = "value2";

    Map<String, String> config = ImmutableMap.of(key1, anotherValue1);
    Map<String, String> anotherConfig = ImmutableMap.of(key1, value1, key2, value2);

    Release releaseWithHighPriority = new Release();
    releaseWithHighPriority.setConfigurations(gson.toJson(config));

    Release releaseWithLowPriority = new Release();
    releaseWithLowPriority.setConfigurations(gson.toJson(anotherConfig));

    Map<String, String> result =
        configController.mergeReleaseConfigurations(
            Lists.newArrayList(releaseWithHighPriority, releaseWithLowPriority));

    assertEquals(2, result.keySet().size());
    assertEquals(anotherValue1, result.get(key1));
    assertEquals(value2, result.get(key2));
  }

  @Test(expected = JsonSyntaxException.class)
  public void testTransformConfigurationToMapFailed() throws Exception {
    String someInvalidConfiguration = "xxx";
    Release someRelease = new Release();
    someRelease.setConfigurations(someInvalidConfiguration);

    configController.mergeReleaseConfigurations(Lists.newArrayList(someRelease));
  }

  private AppNamespace assmbleAppNamespace(String appId, String namespace) {
    AppNamespace appNamespace = new AppNamespace();
    appNamespace.setAppId(appId);
    appNamespace.setName(namespace);
    return appNamespace;
  }
}
