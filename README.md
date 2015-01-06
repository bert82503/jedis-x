jedis-x
=======

The extension of [Jedis](https://github.com/xetorthio/jedis) framework.<br>
[Jedis 源码剖析笔记](https://github.com/EdwardLee03/jedis-sr)

#Reference
[1] [Redis](http://redis.io)<br>
[2] [Redis - @江南白衣](https://github.com/springside/springside4/wiki/Redis)

#Change log
### 2015.1.6
* [Redis集群] 调整整个Redis集群节点的有效性检测方式，由基于Commons Pool 2的"空闲对象驱逐检测机制"改为"定期的Redis服务器状态检测机制"<br>

### 2014.12.27
* [Redis集群] 增加Redis服务定义及基于Jedis的自定义服务实现类<br>
* [Redis集群] 基于Jedis定制实现的"Redis服务器异常(宕机)时自动摘除，恢复正常时自动添加"的功能<br>
* [Redis集群] 基于Spring工厂Bean实现的"自定义数据分片的Jedis连接池"工厂<br>
* [Memcached集群] 增加基于Spy Memcached的缓存服务及其实现<br>
* [缓存迁移] 增加Redis缓存适配器和缓存迁移服务及其实现<br>
