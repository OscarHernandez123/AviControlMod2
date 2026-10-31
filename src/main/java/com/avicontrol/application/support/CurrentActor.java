package com.avicontrol.application.support;

import java.util.Set;

public record CurrentActor(String id, Set<String> authorities) {}
