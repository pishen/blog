name := "blog"

scalaVersion := "2.13.11"

enablePlugins(ParadoxPlugin)

enablePlugins(ParadoxMaterialThemePlugin)

Compile / paradoxMaterialTheme := {
  ParadoxMaterialTheme()
    .withCopyright("© 2023 Pishen Tsai")
    .withFavicon("assets/favicon-32x32.png")
    .withCustomStylesheet("assets/custom.css")
    .withoutSearch()
    .withRepository(uri("https://github.com/pishen/blog"))
}
