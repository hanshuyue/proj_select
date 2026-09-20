---
name: dict-ppt-template
description: Maintain, debug, or migrate this project's form-driven DICT PowerPoint generation, including dynamic tables, selected-mode pages, template typography, merges, overflow, attachments, and template replacement.
---

# DICT PPT template generation

Use the web form as the source of truth and the uploaded PPTX as the visual source of truth. Preserve existing user changes and do not encode sample text from screenshots as business data.

Before changing generation logic, identify the form state in `scaffold-vue/src/views/initiation/form.vue`, the PPT mutation in `InitiationPptService.java`, and the active template configured by `scaffold.initiation.bootstrap-template-path`.

For ordinary fixes, follow [references/template-playbook.md](references/template-playbook.md). Read its migration checklist whenever a template is replaced, and its troubleshooting section for overlap, missing rows, incorrect fonts, broken merges, or corrupt output.

Validate by building Vue, clean-compiling Java, generating representative multi-row/multi-mode data for layout changes, and inspecting affected slide XML or rendered slides. Prefer semantic table/header detection over fixed slide numbers. When fixed positions are unavoidable, record them in the playbook with the visible marker that validates the assumption.
