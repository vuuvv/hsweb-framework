package org.hswebframework.web;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhouhao
 * @since 3.0.6
 */
@Slf4j
public abstract class ModuleUtils {

    private ModuleUtils() {

    }

    private final static Map<Class, ModuleInfo> classModuleInfoRepository;

    private final static Map<String, ModuleInfo> nameModuleInfoRepository;

    static {
        classModuleInfoRepository = new ConcurrentHashMap<>();
        nameModuleInfoRepository = new ConcurrentHashMap<>();
        try {
            log.info("init module info");
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:/hsweb-module.json");
            for (Resource resource : resources) {
                String classPath = getClassPath(resource.getURL().getPath(), "hsweb-module.json");
                ModuleInfo moduleInfo = JSON.parseObject(resource.getInputStream(), ModuleInfo.class);
                moduleInfo.setClassPath(classPath);
                ModuleUtils.register(moduleInfo);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static ModuleInfo getModuleByClass(Class type) {
        return classModuleInfoRepository.computeIfAbsent(type, ModuleUtils::parse);
    }

    public static String getClassPath(Class type) {
        String path = type.getResource("").getPath();
        String packages = type.getPackage().getName();
        return getClassPath(path, packages);
    }

    public static String getClassPath(String path, String packages) {
        int pos = path.contains("!/") ? 3 : path.endsWith("/") ? 2 : 1;
        return path.substring(0, path.length() - packages.length() - pos);
    }

    private static ModuleInfo parse(Class type) {
        String classpath = getClassPath(type);
        return nameModuleInfoRepository.values()
                .stream()
                .filter(moduleInfo -> classpath.equals(moduleInfo.classPath))
                .findFirst()
                .orElse(noneInfo);
    }

    public static ModuleInfo getModule(String id) {
        return nameModuleInfoRepository.get(id);
    }

    public static void register(ModuleInfo moduleInfo) {
        nameModuleInfoRepository.put(moduleInfo.getId(), moduleInfo);
    }

    private static final ModuleInfo noneInfo = new ModuleInfo();

    @Getter
    @Setter
    public static class ModuleInfo {

        private String classPath;

        private String id;

        private String groupId;

        private String artifactId;

        private String gitCommitHash;

        private String gitRepository;

        private String comment;

        private String version;

        public String getId() {
            if (StringUtils.isEmpty(id)) {
                id = groupId + "/" + artifactId;
            }
            return id;
        }

        public boolean isNone() {
            return StringUtils.isEmpty(classPath);
        }
    }
}