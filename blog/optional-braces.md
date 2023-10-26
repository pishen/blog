---
title: Scala 3 消失的大括號
date: 2023-09-10
---

說到 Scala 3 的新語法，最有特徵也最具爭議的，應該就是「用縮排取代大括號」的設計了。自從 Martin 在 2017 年五月拋出[第一個提案](https://github.com/lampepfl/dotty/issues/2491)之後，批評的聲音就不曾中斷過。有人說「你要模仿 Python 的話，乾脆把 dynamic typing 也加上去好了」，也有人說「按照這個邏輯，我們應該把道路上的標線跟紅綠燈也拿掉，大家只要在腦中記得交通規則，就能有看起來簡潔又清爽的道路」。但是，身為 Scala 大家長的 Martin，依然 ~~我行我素~~ 擇善固執地帶領 Scala 開發團隊，將這個「最能一眼看出是 Scala 3」的語法，加入了正式版本當中，並且包括官方的[教科書](https://www.artima.com/shop/programming_in_scala_5ed)與[線上課程](https://www.coursera.org/specializations/scala)，也都預設採用了這個語法。

看來當初大家以為的玩笑，轉眼間已成了事實。不過既然木已成舟了，就讓我們來看看葫蘆裡賣什麼藥吧。

## 核心概念

完整的大括號省略規則其實相當複雜，細節可以參考官方的 [Optional Braces](https://docs.scala-lang.org/scala3/reference/other-new-features/indentation.html) 頁面。不過，核心概念並不難懂，大致可以理解成：

*當我們有一段用大括號包起來的 code，並且有按照規矩好好縮排，那這對大括號可以省略不寫*

這邊說的規矩，其實就是從 Scala 2 以來大家習慣的縮排方式，也就是說只要我們原來有維持縮排的好習慣，或是有用 scalafmt 之類的工具在做整理，Scala 3 就是允許你現在可以把 code 裡的大括號拿掉了而已(不過還是有些例外，像是 `class`/`object`/`trait` 的宣告，以及 `.map {...}` 的大括號就需要搭配冒號才能拿掉)。

例如下面這段 code
```scala
val sum = {
  val x = 1
  val y = 2
  x + y
}
```
在 Scala 3 就可以改寫成:
```scala
val sum =
  val x = 1
  val y = 2
  x + y
```

這個設計其實不算新鮮事，我們都知道 Scala 的行尾分號跟 Java 不同，是可以省略不寫的，當我們有乖乖換行時，就可以選擇把 `;` 省略不寫。現在只是把這個概念延伸到「當我們有乖乖縮排時，就可以選擇把 `{}` 也省略不寫」而已。而且，Scala 2 的大括號在某些情況下，本來就已經可以省略不寫了，例如大括號中只有包含一個 statement/expression 時，或是大括號接在 `case ... =>` 後方時。由此可見，省略大括號的努力，其實是從 Scala 2 就已經開始了。

## 細部規則
這裡稍微介紹一下細部規則，但與其看一堆規則，還是建議直接下去寫寫看才是最快的，反正寫錯了 compiler 都會提醒你，這就是靜態語言方便的地方。

### 插入大括號的時機
當我們把大括號省略不寫時，compiler 會在特定的行尾位置自動插入 `<indent>` 與 `<outdent>` 的隱藏字元，這兩個字元的語意等同於 `{` 與 `}`，所以也可以理解成 compiler 自動幫你插入大括號的時機。

當下面兩個條件同時滿足時，在當前的行尾插入一個 `<indent>`:

1. 當前的行尾是一個本來可以寫 `{` 的位置，例如用以下關鍵字結尾的行:
```
=  =>  ?=>  <-  catch  do  else  finally  for
if  match  return  then  throw  try  while  yield
```
或是 if condition 後面的 `)`、class 宣告後面的 `:`、given instance 後面的 `with`...等等。

2. 下一行的起始位置相對當前行有更深的縮排(多一個空格就算，但 Scala 習慣兩個)

當進入 `<indent>` 後的縮排區域後，compiler 會把第一行的縮排深度記到一個 stack 中，稱作「當前縮排深度」，直到遇到某一行的縮排深度比當前縮排深度還淺時，才會在前一行的行尾加上 `<outdent>`，並且 pop 掉 stack 最上層的數字。也就是以下的 code:

```scala
val sum =
  val x = 1
    val y = 2
  x + y
println(sum)
```

會被 compiler 自動改寫成:

```scala
val sum ={     //當前縮排深度: 0
  val x = 1    //當前縮排深度: 2
    val y = 2  //當前縮排深度: 2
  x + y}       //當前縮排深度: 2
println(sum)   //當前縮排深度: 0
```

可以注意到雖然 `val y = 2` 有更深的縮排，但因為前一行不符合插入 `<indent>` 的條件，所以他依然算在同一層縮排區域中(當然是不建議這樣寫，縮排就是要整齊)。

### Template body 的大括號
接在 `class`, `object`, `trait`, `enum`, `package` 後面的大括號，Scala 稱作 template body，要省略這部分的大括號時，需要在第一行的行尾額外加上一個 `:`，也就是以下的 code:

```scala
class C(x: Int) extends A:
  def f = x
```

會被 compiler 自動改寫成:

```scala
class C(x: Int) extends A{
  def f = x}
```

### Function arguments 的大括號
在 Scala 中，當我們呼叫一個 function 時，可以選擇把呼叫時的小括號改成大括號，並且獲得一些額外的效果，例如:

```scala
println{
  val x = 1
  val y = 2
  x + y
}

xs.map {
  x =>
    val y = x - 1
    y * y
}
```

自從 Scala 3.3 之後，這類的大括號也變成可以省略了，但一樣要搭配一個冒號:

```scala
println:
  val x = 1
  val y = 2
  x + y

xs.map:
  x =>
    val y = x - 1
    y * y
```

甚至在第二個例子中，我們還可以進一步把 `x =>` 搬到冒號後方(如果有 `case` 的話不適用):

```scala
xs.map: x =>
  val y = x - 1
  y * y
```

## 優點與缺點

不可否認的，可省略的大括號進一步縮短了 Scala 程式碼的長度，可以預期未來不會再看到 code 裡面出現像是這樣的東西:
```
        }
      }
    }
  }
}
```
以精簡的效果來說算是顯著的。同時，Scala 3 並沒有強迫使用者一定要改變習慣，既有的大括號語法依然可以在 Scala 3 中使用，可說是一個柔和的導入方式，讓大家可以根據當下的需要選擇適合的寫法。不過，缺點也是顯而易見的，那就是 Scala 3 將會有更多風格迥異的程式碼混雜在一起，增加 programmer 的理解負擔。另外，少了大括號的可見界線，改用隱形的換行符號來分隔不同層級的 scope，是否會影響易讀性，甚至造成一些 bug 更難被一眼看出來，也是要實際導入一陣子後才會知道結果。

Monix 的作者 Alexandru Nedelcu 就有在他的部落格中兩次提到他為何反對這樣的設計:

- [On Scala 3's Optional Braces](https://alexn.org/blog/2022/10/24/scala-3-optional-braces/)
- [Scala 3 Significant Indentation Woes: Sample](https://alexn.org/blog/2023/06/06/scala-3-significant-indentation-woes-sample/)

與此同時，Martin 也有提到他為何認為這依然是個正確的方向:

- [Feedback sought: Optional Braces](https://contributors.scala-lang.org/t/feedback-sought-optional-braces/4702)

看來只有時間才能證明一切了。(我是蠻認同就算這東西長得像 Python，也不會吸引到 Python 工程師來用就是了lol)
