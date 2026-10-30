# Eryx9 Development Testing Mode

Normal startup is unchanged.

To launch directly into Eryx9 for development testing, use one of:

- Java system property:
  `java -Deryx9=true -jar ...` or `-Dspaceexplorer.eryx9=true`
- Command-line argument:
  `--eryx9`

Example:

`java -Deryx9=true -cp out com.spaceexplorer.Game`

When enabled, the game initializes the same planetary world, player, camera, chunk manager, and renderer used by the normal shuttle transition, but skips the spaceship and loading sequence.

The shuttle interaction remains the only normal-gameplay way to travel to Eryx9.