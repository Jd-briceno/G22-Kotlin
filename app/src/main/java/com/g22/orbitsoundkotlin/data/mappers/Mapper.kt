package com.g22.orbitsoundkotlin.data.mappers

/**
 * Interfaz genérica Mapper para conversión de datos entre capas.
 * Sigue el patrón Mapper para separación de responsabilidades.
 * 
 * @param I Input (tipo de entrada - ej: JSON, DTO)
 * @param O Output (tipo de salida - ej: Domain Model)
 */
interface Mapper<I, O> {
    /**
     * Transforma un objeto de entrada en un objeto de salida.
     * 
     * @param input Objeto de entrada a transformar
     * @return Objeto de salida transformado
     */
    fun map(input: I): O
}

