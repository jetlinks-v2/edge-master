# 边缘网关，平台管理端模块


## 使用

1. 关联模块到拓展模块中

```bash

## 1.添加core模块, 如果已经添加类edge-core模块,则可以忽略此步骤.
$  git submodule add --force git@github.com:jetlinks/edge-core.git expands-components/jetlinks-edge/edge-core

## 2. 添加边缘云端管理模块

$  git submodule add --force git@github.com:jetlinks/edge-master.git expands-components/jetlinks-edge/edge-master

```

2. 在项目根目录下的`pom.xml`中的`modules`节点中添加模块

```xml
<modules>
    <module>expands-components/jetlinks-edge/edge-master</module>
</modules>
```

3. 在启动模块中引入依赖

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>edge-master</artifactId>
    <version>${project.version}</version>
</dependency>
```

4. 重新编译启动即可
