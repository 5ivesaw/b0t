# SawBotV1 tools

Development and runtime-validation utilities that are not packed into the Minecraft mod JAR.

- `dataset-validator/validate_telemetry.py`: validate, summarize, and recover `.sbt` trajectories.
- `replay-inspector/inspect_telemetry.py`: inspect aligned observation/input/outcome records.
- `dummy-model/dummy_model.py`: Phase 4 `sawbot.bridge/0.1` reference server.
- `dummy-model/RUN-DUMMY-MODEL.bat`: interactive local model commands.
- `dummy-model/RUN-ACTUATOR-DEMO.bat`: deterministic legitimate-control demo.

Run the protocol-only self-test with:

```bash
python sawbot-tools/dummy-model/dummy_model.py --self-test
```
