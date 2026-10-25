/**
 * Cross-cutting technical code (logging, later error handling). Open module: any business module may use it,
 * and it must never depend on a business module.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Shared", type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.dowglasmaia.financecontroller.shared;
