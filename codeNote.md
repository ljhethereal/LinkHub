字段	常用长度	原因
用户名 username	50 / 32	短标识，够用
密码 password	64	存哈希值长度固定：MD5=32、SHA-256=64;BCrypt=60。留 64~72 兜底
邮箱 email	255	RFC 标准邮箱最大约 254 字符
手机号	20	含国际区号 +86 等
昵称 / 标题 title	100 / 128	展示用短文本
URL (头像、链接)	255 / 512	URL 可能较长，255 常见，严谨点用 512
简介 bio、地址	255 / 500	短描述
长正文 content	用 TEXT, 不设 length	超过几千字符就别用 varchar
状态 / 类型枚举	20 / 32	存 "ACTIVE"、"DELETED" 等
UUID	36	标准 UUID 带连字符是 36 位