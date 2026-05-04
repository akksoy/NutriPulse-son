# Package migration

This repo uses `com.nutripulse.app` as the canonical package root.

To physically move legacy source trees from `com.rasyonpro.app` into `com.nutripulse.app`, run:

```bash
bash scripts/move_rasyonpro_to_nutripulse.sh
```

After running it, verify with:

```bash
grep -R "com.rasyonpro.app" app/src/main/java app/src/test/java app/src/androidTest/java
```

Expected result: no matches.
