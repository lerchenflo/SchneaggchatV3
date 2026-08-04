# Agent Rules & Guidelines

- **Never hardcode colors**: Always use `MaterialTheme.colorScheme` colors (or `contentColorFor(...)`). If a specific non-theme color is needed, ask the user first.
- **String resources**: Add new strings ONLY to the default `strings.xml` (in `values/`). NEVER add strings to German locale files like `values-de` or `values-de-rAT` (`strings-de` / `strings-de-at`).
- **Never run Gradle builds**: Do not run `./gradlew` commands or build tasks unless explicitly requested by the user.
- **Do not assume anything**: If an instruction or requirement is ambiguous or unclear, ask the user for clarification before proceeding.
- **Use available skills**: Always check and apply relevant skills from the skills directory when working on tasks.
