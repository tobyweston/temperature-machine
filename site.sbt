import microsites.MicrositeFavicon

lazy val site = (project in file("site"))
  .enablePlugins(MicrositesPlugin)
  .settings(siteSettings)

lazy val siteSettings = Seq(
  micrositeName := "Temperature Machine",
  micrositeDescription := "The homebrew data logger",

  micrositeHomepage := "http://baddotrobot.com",
  micrositeAuthor := "Toby Weston",
  micrositeGithubOwner := "tobyweston",
  micrositeGithubRepo := "temperature-machine",
  micrositeBaseUrl := "temperature-machine",
  micrositeHighlightTheme := "atom-one-light",
  micrositeDocumentationUrl := "/docs",

  micrositeFavicons := Seq(
    MicrositeFavicon("favicon16x16.png", "16x16"),
    MicrositeFavicon("favicon24x24.png", "24x24"),
    MicrositeFavicon("favicon32x32.png", "32x32")
  ),

  micrositePalette := Map(
    "brand-primary"   -> "#5B5988",
    "brand-secondary" -> "#292E53",
    "brand-tertiary"  -> "#222749",
    "gray-dark"       -> "#49494B",
    "gray"            -> "#7B7B7E",
    "gray-light"      -> "#E5E5E6",
    "gray-lighter"    -> "#F4F3F4",
    "white-color"     -> "#FFFFFF"),

  fork in tut := true
)