

## Excel 命令：

### 替换字符串
```
=SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(TEXT(A1,"hh时mm分ss秒"),"秒",""),"分","*60+"),"时","*3600+"),"天","*3600*24+")


```