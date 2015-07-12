/*
 * RingBuffer.js
 */
/* jshint browser:true */
define([], function () {
        'use strict';

        /**
         * A RingBuffer.
         * @param {!number} capacity
         * @template T
         * @constructor
         */
        function RingBuffer(capacity) {
            this.capacity = capacity;
            this.writeIndex = 0;
            this.buffer = [];
            this.capacityBaseZero = this.capacity - 1;
        }

        /**
         * Adds an item to the buffer.
         * @param {T} value
         */
        RingBuffer.prototype.add = function (value) {
            this.buffer[this.writeIndex] = value;
            this.writeIndex = (this.writeIndex + 1) % this.capacity;
        };

        /**
         * Converts buffer to an Array.
         * @returns {Array.<T>}
         * @nosideeffects
         */
        RingBuffer.prototype.toArray = function () {
            if (this.buffer.length !== this.capacity) {
                return this.buffer.slice();
            }
            return this.buffer.slice(this.writeIndex)
                .concat(this.buffer.slice(0, this.writeIndex));
        };

        /**
         * Iterates over elements of buffer invoking iteratee for each element.
         * @param {function(T): boolean} iteratee
         */
        RingBuffer.prototype.forEach = function (iteratee) {
            if (this.buffer.length !== this.capacity) {
                this.buffer.forEach(iteratee);

            } else {
                var i = this.writeIndex;
                do {
                    if (!iteratee(this.buffer[i])) {
                        return;
                    }
                    i = (i + 1) % this.capacity;
                } while (i !== this.writeIndex);
            }
        };
        /**
         * Iterates over elements in reverse order invoking iteratee for each element.
         * @param {function(T): boolean} iteratee
         */
        RingBuffer.prototype.forEachReverse = function (iteratee) {
            var i = this.writeIndex - 1;
            if (this.buffer.length !== this.capacity) {
                while (i >= 0) {
                    if (!iteratee(this.buffer[i])) {
                        return;
                    }
                    i--;
                }

            } else {
                var writeIndexBaseZero = this.writeIndex - 1;
                do {
                    if (i < 0) {
                        i = this.capacityBaseZero;
                    }
                    if (!iteratee(this.buffer[i])) {
                        return;
                    }
                    i--;
                } while (i !== writeIndexBaseZero);
            }
        };

        return RingBuffer;
    }
);